package com.valubank.payments.service;

import com.valubank.payments.client.AccountsServiceClient;
import com.valubank.payments.dto.AccountDto;
import com.valubank.payments.dto.PaymentRequest;
import com.valubank.payments.dto.ScheduledPaymentRequest;
import com.valubank.payments.entity.Payment;
import com.valubank.payments.entity.ScheduledPayment;
import com.valubank.payments.exception.AccountNotFoundException;
import com.valubank.payments.exception.AccountNotOwnedException;
import com.valubank.payments.exception.DependencyUnavailableException;
import com.valubank.payments.exception.InvalidScheduledPaymentException;
import com.valubank.payments.exception.ScheduledPaymentNotFoundException;
import com.valubank.payments.exception.ScheduledPaymentStateException;
import com.valubank.payments.exception.SourceAccountUnverifiableException;
import com.valubank.payments.repository.ScheduledPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Payments a customer schedules for a future date.
 *
 * Scheduling only checks the rules that can be checked up front (future date,
 * positive amount, IBAN format, account ownership). Everything else - fraud
 * check, available funds / overdraft limit - is applied at execution time by
 * handing the payment to {@link PaymentService#createPayment}, exactly like an
 * immediate payment. That also records it as a regular Payment, so it shows
 * up in the account's payment history.
 *
 * execute() and cancel() are synchronized so a payment can't be executed
 * twice, or cancelled while it is being executed. Fine for this single-
 * instance workshop service; a multi-instance deployment would need a DB lock.
 */
@Service
public class ScheduledPaymentService {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_EXECUTED = "EXECUTED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final String PAYMENT_STATUS_COMPLETED = "COMPLETED";

    // Same structural check as the frontend (frontend/src/utils/iban.js) - no mod-97 checksum,
    // because ValuBank's own fictional IBANs wouldn't pass it.
    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");

    private static final Logger log = LoggerFactory.getLogger(ScheduledPaymentService.class);

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final PaymentService paymentService;
    private final AccountsServiceClient accountsServiceClient;
    private final Clock clock;

    public ScheduledPaymentService(ScheduledPaymentRepository scheduledPaymentRepository,
                                   PaymentService paymentService,
                                   AccountsServiceClient accountsServiceClient,
                                   Clock clock) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
        this.paymentService = paymentService;
        this.accountsServiceClient = accountsServiceClient;
        this.clock = clock;
    }

    /**
     * @throws InvalidScheduledPaymentException   if the request breaks an up-front rule (400)
     * @throws AccountNotOwnedException           if the source account isn't the customer's (403)
     * @throws SourceAccountUnverifiableException if the source account can't be looked up (502)
     */
    public ScheduledPayment schedule(ScheduledPaymentRequest request) {
        if (request.getCustomerId() == null) {
            throw new InvalidScheduledPaymentException("Customer is required");
        }
        if (request.getFromAccountId() == null) {
            throw new InvalidScheduledPaymentException("Source account is required");
        }
        String iban = normalizeIban(request.getToAccountIban());
        if (!IBAN_PATTERN.matcher(iban).matches()) {
            throw new InvalidScheduledPaymentException("Beneficiary IBAN is not a valid IBAN");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidScheduledPaymentException("Amount must be greater than zero");
        }
        if (request.getExecutionDate() == null || !request.getExecutionDate().isAfter(today())) {
            throw new InvalidScheduledPaymentException("Execution date must be in the future");
        }

        AccountDto account;
        try {
            account = accountsServiceClient.getAccount(request.getFromAccountId());
        } catch (AccountNotFoundException | DependencyUnavailableException e) {
            throw new SourceAccountUnverifiableException("Could not verify source account");
        }
        if (!request.getCustomerId().equals(account.getCustomerId())) {
            throw new AccountNotOwnedException("Source account does not belong to this customer");
        }

        ScheduledPayment scheduledPayment = new ScheduledPayment();
        scheduledPayment.setCustomerId(request.getCustomerId());
        scheduledPayment.setFromAccountId(request.getFromAccountId());
        scheduledPayment.setToAccountIban(iban);
        scheduledPayment.setToAccountName(request.getToAccountName());
        scheduledPayment.setAmount(request.getAmount());
        scheduledPayment.setCurrency(account.getCurrency());
        scheduledPayment.setDescription(request.getDescription());
        scheduledPayment.setExecutionDate(request.getExecutionDate());
        scheduledPayment.setStatus(STATUS_SCHEDULED);
        scheduledPayment.setCreatedAt(Instant.now(clock));
        return scheduledPaymentRepository.save(scheduledPayment);
    }

    public List<ScheduledPayment> getScheduledPaymentsForCustomer(Long customerId) {
        return scheduledPaymentRepository.findByCustomerIdOrderByExecutionDateAscIdAsc(customerId);
    }

    /**
     * @throws ScheduledPaymentNotFoundException if it doesn't exist or isn't this customer's (404)
     * @throws ScheduledPaymentStateException    if it is no longer SCHEDULED (409)
     */
    public synchronized ScheduledPayment cancel(Long id, Long customerId) {
        ScheduledPayment scheduledPayment = findById(id);
        if (!scheduledPayment.getCustomerId().equals(customerId)) {
            throw new ScheduledPaymentNotFoundException("Scheduled payment " + id + " not found");
        }
        String status = scheduledPayment.getStatus();
        if (STATUS_EXECUTED.equals(status) || STATUS_CANCELLED.equals(status)) {
            throw new ScheduledPaymentStateException(
                    "Scheduled payment is " + status + " and can no longer be cancelled");
        }
        scheduledPayment.setStatus(STATUS_CANCELLED);
        scheduledPayment.setCancelledAt(Instant.now(clock));
        return scheduledPaymentRepository.save(scheduledPayment);
    }

    /**
     * Executes a scheduled payment now, regardless of its execution date. Used by
     * the daily run for due payments, and by the test-only "execute now" endpoint.
     *
     * @throws ScheduledPaymentNotFoundException if it doesn't exist (404)
     * @throws ScheduledPaymentStateException    if it is no longer SCHEDULED (409)
     */
    public synchronized ScheduledPayment execute(Long id) {
        ScheduledPayment scheduledPayment = findById(id);
        if (!STATUS_SCHEDULED.equals(scheduledPayment.getStatus())) {
            throw new ScheduledPaymentStateException(
                    "Scheduled payment is " + scheduledPayment.getStatus() + " and can no longer be executed");
        }

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setFromAccountId(scheduledPayment.getFromAccountId());
        paymentRequest.setToAccountIban(scheduledPayment.getToAccountIban());
        paymentRequest.setToAccountName(scheduledPayment.getToAccountName());
        paymentRequest.setAmount(scheduledPayment.getAmount());
        paymentRequest.setCurrency(scheduledPayment.getCurrency());
        paymentRequest.setDescription(scheduledPayment.getDescription());

        scheduledPayment.setExecutedAt(Instant.now(clock));
        try {
            Payment payment = paymentService.createPayment(paymentRequest);
            scheduledPayment.setPaymentId(payment.getId());
            if (PAYMENT_STATUS_COMPLETED.equals(payment.getStatus())) {
                scheduledPayment.setStatus(STATUS_EXECUTED);
                scheduledPayment.setReason(null);
            } else {
                scheduledPayment.setStatus(STATUS_FAILED);
                scheduledPayment.setReason(payment.getReason());
            }
        } catch (SourceAccountUnverifiableException e) {
            // No Payment is recorded in this case (same as for an immediate payment).
            scheduledPayment.setStatus(STATUS_FAILED);
            scheduledPayment.setReason("Could not verify source account");
        }
        return scheduledPaymentRepository.save(scheduledPayment);
    }

    /**
     * Executes every SCHEDULED payment whose execution date is today or earlier
     * (earlier = the service was down on the day itself).
     */
    public void executeDuePayments() {
        List<ScheduledPayment> due = scheduledPaymentRepository
                .findByStatusAndExecutionDateLessThanEqualOrderByIdAsc(STATUS_SCHEDULED, today());
        for (ScheduledPayment scheduledPayment : due) {
            try {
                ScheduledPayment result = execute(scheduledPayment.getId());
                log.info("Executed scheduled payment {}: {}", result.getId(), result.getStatus());
            } catch (ScheduledPaymentStateException e) {
                // Cancelled or executed (e.g. via the test endpoint) since we queried - nothing to do.
            }
        }
    }

    private ScheduledPayment findById(Long id) {
        return scheduledPaymentRepository.findById(id)
                .orElseThrow(() -> new ScheduledPaymentNotFoundException("Scheduled payment " + id + " not found"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String normalizeIban(String rawIban) {
        return rawIban == null ? "" : rawIban.replaceAll("\\s+", "").toUpperCase();
    }
}
