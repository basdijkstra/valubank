package com.valubank.interestrate.dto;

import java.math.BigDecimal;

/**
 * Response shape returned by the interest-rate endpoints, e.g.
 * {"accountType":"CURRENT","ratePercentage":0.1}
 */
public class InterestRateDto {

    private String accountType;
    private BigDecimal ratePercentage;

    public InterestRateDto() {
    }

    public InterestRateDto(String accountType, BigDecimal ratePercentage) {
        this.accountType = accountType;
        this.ratePercentage = ratePercentage;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }
}
