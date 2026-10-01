package com.valubank.payments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Incoming request body for POST /api/scheduled-payments (from the React frontend).
 * There is no currency field: a scheduled payment is always in the source account's currency.
 */
public class ScheduledPaymentRequest {

    /** The logged-in customer - the source account must belong to them. */
    private Long customerId;
    private Long fromAccountId;
    private String toAccountIban;
    private String toAccountName;
    private BigDecimal amount;
    private String description;
    private LocalDate executionDate;

    public ScheduledPaymentRequest() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDate executionDate) {
        this.executionDate = executionDate;
    }
}
