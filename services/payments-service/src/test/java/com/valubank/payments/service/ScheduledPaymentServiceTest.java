package com.valubank.payments.service;

import com.valubank.payments.client.AccountsServiceClient;
import com.valubank.payments.dto.AccountDto;
import com.valubank.payments.dto.PaymentRequest;
import com.valubank.payments.dto.ScheduledPaymentRequest;
import com.valubank.payments.entity.Payment;
import com.valubank.payments.entity.ScheduledPayment;
import com.valubank.payments.exception.AccountNotOwnedException;
import com.valubank.payments.exception.InvalidScheduledPaymentException;
import com.valubank.payments.exception.ScheduledPaymentNotFoundException;
import com.valubank.payments.exception.ScheduledPaymentStateException;
import com.valubank.payments.exception.SourceAccountUnverifiableException;
import com.valubank.payments.repository.ScheduledPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledPaymentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;
    private static final Long ALICE_CHECKING = 1L;

    private ScheduledPaymentRepository repository;
    private PaymentService paymentService;
    private AccountsServiceClient accountsServiceClient;
    private ScheduledPaymentService service;

    @BeforeEach
    void setUp() {
        repository = mock(ScheduledPaymentRepository.class);
        paymentService = mock(PaymentService.class);
        accountsServiceClient = mock(AccountsServiceClient.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().plusSeconds(3600), ZoneId.of("UTC"));
        service = new ScheduledPaymentService(repository, paymentService, accountsServiceClient, clock);

        when(repository.save(any(ScheduledPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountsServiceClient.getAccount(ALICE_CHECKING)).thenReturn(account(ALICE_CHECKING, ALICE, "EUR"));
    }

    // --- scheduling ---

    @Test
    void schedulesValidPaymentInSourceAccountCurrency() {
        ScheduledPayment result = service.schedule(request(TODAY.plusDays(1), "25.00"));

        assertThat(result.getStatus()).isEqualTo("SCHEDULED");
        assertThat(result.getCurrency()).isEqualTo("EUR");
        assertThat(result.getExecutionDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(result.getToAccountIban()).isEqualTo("NL01VALU0000000003");
        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void normalizesBeneficiaryIban() {
        ScheduledPaymentRequest request = request(TODAY.plusDays(1), "25.00");
        request.setToAccountIban("nl01 valu 0000 0000 03");

        assertThat(service.schedule(request).getToAccountIban()).isEqualTo("NL01VALU0000000003");
    }

    @Test
    void rejectsExecutionDateToday() {
        assertThatThrownBy(() -> service.schedule(request(TODAY, "25.00")))
                .isInstanceOf(InvalidScheduledPaymentException.class)
                .hasMessage("Execution date must be in the future");
    }

    @Test
    void rejectsExecutionDateInThePast() {
        assertThatThrownBy(() -> service.schedule(request(TODAY.minusDays(1), "25.00")))
                .isInstanceOf(InvalidScheduledPaymentException.class);
    }

    @Test
    void rejectsMissingExecutionDate() {
        assertThatThrownBy(() -> service.schedule(request(null, "25.00")))
                .isInstanceOf(InvalidScheduledPaymentException.class);
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> service.schedule(request(TODAY.plusDays(1), "0.00")))
                .isInstanceOf(InvalidScheduledPaymentException.class)
                .hasMessage("Amount must be greater than zero");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> service.schedule(request(TODAY.plusDays(1), "-5")))
                .isInstanceOf(InvalidScheduledPaymentException.class);
    }

    @Test
    void rejectsInvalidIban() {
        ScheduledPaymentRequest request = request(TODAY.plusDays(1), "25.00");
        request.setToAccountIban("not-an-iban");

        assertThatThrownBy(() -> service.schedule(request))
                .isInstanceOf(InvalidScheduledPaymentException.class);
    }

    @Test
    void rejectsSourceAccountOfAnotherCustomer() {
        ScheduledPaymentRequest request = request(TODAY.plusDays(1), "25.00");
        request.setCustomerId(BOB);

        assertThatThrownBy(() -> service.schedule(request))
                .isInstanceOf(AccountNotOwnedException.class);
        verify(repository, never()).save(any());
    }

    // --- cancelling ---

    @Test
    void cancelsScheduledPayment() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "SCHEDULED")));

        ScheduledPayment result = service.cancel(10L, ALICE);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancelledAt()).isNotNull();
    }

    @Test
    void cannotCancelExecutedPayment() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "EXECUTED")));

        assertThatThrownBy(() -> service.cancel(10L, ALICE))
                .isInstanceOf(ScheduledPaymentStateException.class);
    }

    @Test
    void cannotCancelAnotherCustomersPayment() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "SCHEDULED")));

        assertThatThrownBy(() -> service.cancel(10L, BOB))
                .isInstanceOf(ScheduledPaymentNotFoundException.class);
    }

    // --- executing ---

    @Test
    void executesViaRegularPaymentFlow() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "SCHEDULED")));
        when(paymentService.createPayment(any())).thenReturn(payment(99L, "COMPLETED", null));

        ScheduledPayment result = service.execute(10L);

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getPaymentId()).isEqualTo(99L);
        assertThat(result.getExecutedAt()).isNotNull();
        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).createPayment(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("25.00");
        assertThat(captor.getValue().getToAccountIban()).isEqualTo("NL01VALU0000000003");
    }

    @Test
    void rejectedPaymentMarksScheduledPaymentFailedWithReason() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "SCHEDULED")));
        when(paymentService.createPayment(any())).thenReturn(payment(99L, "REJECTED", "Insufficient funds"));

        ScheduledPayment result = service.execute(10L);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getReason()).isEqualTo("Insufficient funds");
        assertThat(result.getPaymentId()).isEqualTo(99L);
    }

    @Test
    void unverifiableSourceAccountMarksScheduledPaymentFailed() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "SCHEDULED")));
        when(paymentService.createPayment(any())).thenThrow(new SourceAccountUnverifiableException("x"));

        ScheduledPayment result = service.execute(10L);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getReason()).isEqualTo("Could not verify source account");
    }

    @Test
    void cannotExecuteTwice() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "EXECUTED")));

        assertThatThrownBy(() -> service.execute(10L))
                .isInstanceOf(ScheduledPaymentStateException.class);
        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void cannotExecuteCancelledPayment() {
        when(repository.findById(10L)).thenReturn(Optional.of(stored(10L, "CANCELLED")));

        assertThatThrownBy(() -> service.execute(10L))
                .isInstanceOf(ScheduledPaymentStateException.class);
        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void executeDuePaymentsOnlyLooksUpPaymentsDueTodayOrEarlier() {
        ScheduledPayment due = stored(10L, "SCHEDULED");
        when(repository.findByStatusAndExecutionDateLessThanEqualOrderByIdAsc("SCHEDULED", TODAY))
                .thenReturn(List.of(due));
        when(repository.findById(10L)).thenReturn(Optional.of(due));
        when(paymentService.createPayment(any())).thenReturn(payment(99L, "COMPLETED", null));

        service.executeDuePayments();

        verify(paymentService, times(1)).createPayment(any());
        assertThat(due.getStatus()).isEqualTo("EXECUTED");
    }

    // --- helpers ---

    private static ScheduledPaymentRequest request(LocalDate executionDate, String amount) {
        ScheduledPaymentRequest request = new ScheduledPaymentRequest();
        request.setCustomerId(ALICE);
        request.setFromAccountId(ALICE_CHECKING);
        request.setToAccountIban("NL01VALU0000000003");
        request.setToAccountName("Bob");
        request.setAmount(new BigDecimal(amount));
        request.setDescription("Rent");
        request.setExecutionDate(executionDate);
        return request;
    }

    private static ScheduledPayment stored(Long id, String status) {
        ScheduledPayment scheduledPayment = new ScheduledPayment();
        scheduledPayment.setId(id);
        scheduledPayment.setCustomerId(ALICE);
        scheduledPayment.setFromAccountId(ALICE_CHECKING);
        scheduledPayment.setToAccountIban("NL01VALU0000000003");
        scheduledPayment.setToAccountName("Bob");
        scheduledPayment.setAmount(new BigDecimal("25.00"));
        scheduledPayment.setCurrency("EUR");
        scheduledPayment.setExecutionDate(TODAY.plusDays(5));
        scheduledPayment.setStatus(status);
        return scheduledPayment;
    }

    private static AccountDto account(Long id, Long customerId, String currency) {
        AccountDto account = new AccountDto();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setCurrency(currency);
        return account;
    }

    private static Payment payment(Long id, String status, String reason) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setStatus(status);
        payment.setReason(reason);
        return payment;
    }
}
