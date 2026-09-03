package com.valubank.accounts.dto;

public class InterestRateResponse {

    private Long accountId;
    private String accountType;
    private double ratePercentage;

    public InterestRateResponse() {
    }

    public InterestRateResponse(Long accountId, String accountType, double ratePercentage) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.ratePercentage = ratePercentage;
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

    public double getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(double ratePercentage) {
        this.ratePercentage = ratePercentage;
    }
}
