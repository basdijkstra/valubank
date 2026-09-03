package com.valubank.accounts.dto;

import com.valubank.accounts.entity.Account;
import com.valubank.accounts.entity.Customer;

import java.math.BigDecimal;

/**
 * Account view for the admin dashboard - includes the owning customer's
 * identity, which the regular AccountDto deliberately omits.
 */
public class AdminAccountDto {

    private Long id;
    private Long customerId;
    private String ownerUsername;
    private String ownerFullName;
    private String iban;
    private String accountType;
    private BigDecimal balance;
    private String currency;

    public AdminAccountDto() {
    }

    public AdminAccountDto(Long id, Long customerId, String ownerUsername, String ownerFullName,
                            String iban, String accountType, BigDecimal balance, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.ownerUsername = ownerUsername;
        this.ownerFullName = ownerFullName;
        this.iban = iban;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
    }

    public static AdminAccountDto from(Account account, Customer owner) {
        return new AdminAccountDto(
                account.getId(),
                account.getCustomerId(),
                owner.getUsername(),
                owner.getFullName(),
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

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
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
