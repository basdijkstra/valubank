package com.valubank.fraud.dto;

/**
 * Response body for POST /api/fraud-checks.
 *
 * <p>{@code reason} is {@code null} when the payment is approved.</p>
 */
public record FraudCheckResponse(
        boolean approved,
        String reason
) {

    public static FraudCheckResponse approve() {
        return new FraudCheckResponse(true, null);
    }

    public static FraudCheckResponse reject(String reason) {
        return new FraudCheckResponse(false, reason);
    }
}
