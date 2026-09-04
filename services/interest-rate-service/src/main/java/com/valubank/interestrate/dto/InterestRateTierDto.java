package com.valubank.interestrate.dto;

import java.math.BigDecimal;

/**
 * One tier of an account type's interest rate schedule, e.g.
 * {"upToAmount":10000.00,"ratePercentage":1.5}
 * upToAmount is omitted/null for the last (unbounded) tier.
 */
public class InterestRateTierDto {

    private BigDecimal upToAmount;
    private BigDecimal ratePercentage;

    public InterestRateTierDto() {
    }

    public InterestRateTierDto(BigDecimal upToAmount, BigDecimal ratePercentage) {
        this.upToAmount = upToAmount;
        this.ratePercentage = ratePercentage;
    }

    public BigDecimal getUpToAmount() {
        return upToAmount;
    }

    public void setUpToAmount(BigDecimal upToAmount) {
        this.upToAmount = upToAmount;
    }

    public BigDecimal getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }
}
