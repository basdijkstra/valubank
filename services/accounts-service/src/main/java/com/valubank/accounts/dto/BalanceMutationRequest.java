package com.valubank.accounts.dto;

import java.math.BigDecimal;

public class BalanceMutationRequest {

    // "DEBIT" or "CREDIT"
    private String type;
    private BigDecimal amount;
    // Currency the amount is denominated in. May differ from the account's own
    // currency, in which case it is converted before being applied.
    private String currency;
    private String reason;

    public BalanceMutationRequest() {
    }

    public BalanceMutationRequest(String type, BigDecimal amount, String currency, String reason) {
        this.type = type;
        this.amount = amount;
        this.currency = currency;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
