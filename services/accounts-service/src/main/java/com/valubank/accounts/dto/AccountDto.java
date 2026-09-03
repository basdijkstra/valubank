package com.valubank.accounts.dto;

import com.valubank.accounts.entity.Account;

import java.math.BigDecimal;

public class AccountDto {

    private Long id;
    private Long customerId;
    private String iban;
    private String accountType;
    private BigDecimal balance;
    private String currency;

    public AccountDto() {
    }

    public AccountDto(Long id, Long customerId, String iban, String accountType, BigDecimal balance, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.iban = iban;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
    }

    public static AccountDto from(Account account) {
        return new AccountDto(
                account.getId(),
                account.getCustomerId(),
                account.getIban(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency()
        );
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
