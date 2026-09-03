package com.valubank.payments.dto;

import java.math.BigDecimal;

/**
 * Incoming request body for POST /api/payments (from the React frontend).
 */
public class PaymentRequest {

    private Long fromAccountId;
    private String toAccountIban;
    private String toAccountName;
    private BigDecimal amount;
    private String currency;
    private String description;

    public PaymentRequest() {
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public String getToAccountIban() {
        return toAccountIban;
    }

    public void setToAccountIban(String toAccountIban) {
        this.toAccountIban = toAccountIban;
    }

    public String getToAccountName() {
        return toAccountName;
    }

    public void setToAccountName(String toAccountName) {
        this.toAccountName = toAccountName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
