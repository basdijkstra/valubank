package com.valubank.accounts.dto;

/**
 * Maps the raw JSON response returned by the separate Interest Rate / Configuration Service:
 * GET http://.../api/interest-rates/{accountType} -> {"accountType":"CHECKING","ratePercentage":0.1}
 */
public class InterestRateServiceRate {

    private String accountType;
    private double ratePercentage;

    public InterestRateServiceRate() {
    }

    public InterestRateServiceRate(String accountType, double ratePercentage) {
        this.accountType = accountType;
        this.ratePercentage = ratePercentage;
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
