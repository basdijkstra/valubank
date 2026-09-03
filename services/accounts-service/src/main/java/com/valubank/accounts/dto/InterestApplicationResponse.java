package com.valubank.accounts.dto;

import java.math.BigDecimal;

/**
 * Result of applying one interest calculation to an account, e.g.
 * {"accountId":1,"accountType":"SAVINGS","previousBalance":10000.00,
 *  "ratePercentage":1.5,"interestAmount":150.00,"newBalance":10150.00,"currency":"EUR"}
 */
public class InterestApplicationResponse {

    private Long accountId;
    private String accountType;
    private BigDecimal previousBalance;
    private BigDecimal ratePercentage;
    private BigDecimal interestAmount;
    private BigDecimal newBalance;
    private String currency;

    public InterestApplicationResponse() {
    }

    public InterestApplicationResponse(Long accountId, String accountType, BigDecimal previousBalance,
                                        BigDecimal ratePercentage, BigDecimal interestAmount,
                                        BigDecimal newBalance, String currency) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.previousBalance = previousBalance;
        this.ratePercentage = ratePercentage;
        this.interestAmount = interestAmount;
        this.newBalance = newBalance;
        this.currency = currency;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance;
    }

    public BigDecimal getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
