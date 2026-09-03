package com.valubank.payments.dto;

import java.math.BigDecimal;

/**
 * Local, hand-written representation of the account JSON shape returned by
 * the Accounts Service. This is intentionally NOT a shared/generated client
 * type - there is no formal contract between the two services, just a
 * plain HTTP call, mirroring how these two independently-built services
 * would really interact.
 */
public class AccountDto {

    private Long id;
    private Long customerId;
    private String iban;
    private String accountType;
    private BigDecimal balance;
    private String currency;

    public AccountDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
