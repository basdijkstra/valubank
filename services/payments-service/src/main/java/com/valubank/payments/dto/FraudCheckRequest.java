package com.valubank.payments.dto;

import java.math.BigDecimal;

/**
 * Request body sent to the Fraud Service: POST /api/fraud-checks
 */
public class FraudCheckRequest {

    private Long fromAccountId;
    private String toAccountIban;
    private BigDecimal amount;

    public FraudCheckRequest() {
    }

    public FraudCheckRequest(Long fromAccountId, String toAccountIban, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountIban = toAccountIban;
        this.amount = amount;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
