package com.valubank.accounts.dto;

import java.math.BigDecimal;

public class BalanceMutationRequest {

    // "DEBIT" or "CREDIT"
    private String type;
    private BigDecimal amount;
    private String reason;

    // Currency the amount is denominated in, as the caller sees it. Nullable -
    // callers that don't send it are assumed to already be in the account's
    // own currency, so no conversion is attempted.
    private String currency;

    public BalanceMutationRequest() {
    }

    public BalanceMutationRequest(String type, BigDecimal amount, String reason) {
        this.type = type;
        this.amount = amount;
        this.reason = reason;
    }

    public BalanceMutationRequest(String type, BigDecimal amount, String reason, String currency) {
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.currency = currency;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
