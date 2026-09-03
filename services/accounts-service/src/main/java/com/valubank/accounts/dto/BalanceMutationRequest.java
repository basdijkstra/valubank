package com.valubank.accounts.dto;

import java.math.BigDecimal;

public class BalanceMutationRequest {

    // "DEBIT" or "CREDIT"
    private String type;
    private BigDecimal amount;
    private String reason;

    public BalanceMutationRequest() {
    }

    public BalanceMutationRequest(String type, BigDecimal amount, String reason) {
        this.type = type;
        this.amount = amount;
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
