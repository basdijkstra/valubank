package com.valubank.payments.dto;

/**
 * Response body returned by the Fraud Service: POST /api/fraud-checks
 */
public class FraudCheckResponse {

    private boolean approved;
    private String reason;

    public FraudCheckResponse() {
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
