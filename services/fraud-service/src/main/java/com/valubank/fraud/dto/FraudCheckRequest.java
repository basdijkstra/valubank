package com.valubank.fraud.dto;

import java.math.BigDecimal;

/**
 * Request body for POST /api/fraud-checks.
 *
 * <p>Matches the contract expected by the Payments Service exactly:</p>
 * <pre>{"fromAccountId":1,"toAccountIban":"NL02VALU0000000009","amount":150.00}</pre>
 */
public record FraudCheckRequest(
        Long fromAccountId,
        String toAccountIban,
        BigDecimal amount
) {
}
