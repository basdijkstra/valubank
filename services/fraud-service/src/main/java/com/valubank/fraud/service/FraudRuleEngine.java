package com.valubank.fraud.service;

import com.valubank.fraud.dto.FraudCheckRequest;
import com.valubank.fraud.dto.FraudCheckResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Evaluates a small set of hardcoded fraud rules against a payment.
 *
 * <p>Rules are evaluated in order and the first matching rule wins. This
 * class deliberately has no persistence and no external dependencies, so it
 * is easy to unit test in isolation and easy to extend with more rules for
 * workshop purposes.</p>
 */
@Component
public class FraudRuleEngine {

    /** Maximum amount allowed per transaction. */
    static final BigDecimal MAX_AMOUNT_PER_TRANSACTION = new BigDecimal("10000");

    /** Destination IBANs that are always flagged as fraudulent. */
    static final Set<String> BLOCKED_IBANS = Set.of(
            "NL99BLOCKED0000000"
    );

    public FraudCheckResponse evaluate(FraudCheckRequest request) {
        if (request.amount().compareTo(MAX_AMOUNT_PER_TRANSACTION) > 0) {
            return FraudCheckResponse.reject(
                    "Amount exceeds maximum allowed per transaction (10000)");
        }

        if (BLOCKED_IBANS.contains(request.toAccountIban())) {
            return FraudCheckResponse.reject(
                    "Destination account is flagged for fraud");
        }

        return FraudCheckResponse.approve();
    }
}
