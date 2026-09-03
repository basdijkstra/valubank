package com.valubank.payments.service;

import com.valubank.payments.client.AccountsServiceClient;
import com.valubank.payments.client.FraudServiceClient;
import com.valubank.payments.dto.AccountDto;
import com.valubank.payments.dto.FraudCheckResponse;
import com.valubank.payments.dto.PaymentRequest;
import com.valubank.payments.entity.Payment;
import com.valubank.payments.exception.AccountNotFoundException;
import com.valubank.payments.exception.DependencyUnavailableException;
import com.valubank.payments.exception.InsufficientFundsException;
import com.valubank.payments.exception.SourceAccountUnverifiableException;
import com.valubank.payments.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the payment flow described in the workshop spec:
 *  1. verify the source account with the Accounts Service
 *  2. reject up-front on insufficient funds (no fraud check needed)
 *  3. run a fraud check
 *  4. debit the source account
 *  5. record the outcome as a Payment, whatever it was
 */
@Service
public class PaymentService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_FAILED = "FAILED";

    private final PaymentRepository paymentRepository;
    private final AccountsServiceClient accountsServiceClient;
    private final FraudServiceClient fraudServiceClient;

    public PaymentService(PaymentRepository paymentRepository,
                           AccountsServiceClient accountsServiceClient,
                           FraudServiceClient fraudServiceClient) {
        this.paymentRepository = paymentRepository;
        this.accountsServiceClient = accountsServiceClient;
        this.fraudServiceClient = fraudServiceClient;
    }

    /**
     * @throws SourceAccountUnverifiableException if the source account can't be verified -
     *                                             in that case no Payment is saved and the
     *                                             controller must respond 502.
     */
    public Payment createPayment(PaymentRequest request) {
        AccountDto sourceAccount;
        try {
            sourceAccount = accountsServiceClient.getAccount(request.getFromAccountId());
        } catch (AccountNotFoundException | DependencyUnavailableException e) {
            throw new SourceAccountUnverifiableException("Could not verify source account");
        }

        Payment payment = new Payment();
        payment.setFromAccountId(request.getFromAccountId());
        payment.setToAccountIban(request.getToAccountIban());
        payment.setToAccountName(request.getToAccountName());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        payment.setTimestamp(Instant.now());

        // b. Reject up-front on insufficient funds - no fraud check in this case.
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            payment.setStatus(STATUS_REJECTED);
            payment.setReason("Insufficient funds");
            return paymentRepository.save(payment);
        }

        // c. Run the fraud check.
        FraudCheckResponse fraudCheck;
        try {
            fraudCheck = fraudServiceClient.checkFraud(
                    request.getFromAccountId(), request.getToAccountIban(), request.getAmount());
        } catch (DependencyUnavailableException e) {
            payment.setStatus(STATUS_FAILED);
            payment.setReason("Fraud service unavailable");
            return paymentRepository.save(payment);
        }

        if (!fraudCheck.isApproved()) {
            payment.setStatus(STATUS_REJECTED);
            payment.setReason(fraudCheck.getReason());
            return paymentRepository.save(payment);
        }

        // d. Fraud check approved - debit the source account.
        try {
            accountsServiceClient.debit(
                    request.getFromAccountId(),
                    request.getAmount(),
                    "Payment to " + request.getToAccountIban());
        } catch (InsufficientFundsException e) {
            payment.setStatus(STATUS_REJECTED);
            payment.setReason("Insufficient funds");
            return paymentRepository.save(payment);
        } catch (DependencyUnavailableException e) {
            payment.setStatus(STATUS_FAILED);
            payment.setReason("Accounts service unavailable");
            return paymentRepository.save(payment);
        }

        // e. Debit succeeded.
        payment.setStatus(STATUS_COMPLETED);
        payment.setReason(null);
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsForAccount(Long accountId) {
        return paymentRepository.findByFromAccountIdOrderByTimestampDesc(accountId);
    }
}
