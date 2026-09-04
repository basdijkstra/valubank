package com.valubank.accounts.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps the raw JSON response returned by the separate Interest Rate / Configuration Service:
 * GET http://.../api/interest-rates/{accountType} ->
 * {"accountType":"SAVINGS","tiers":[{"upToAmount":10000.00,"ratePercentage":1.5},
 *  {"upToAmount":null,"ratePercentage":1.0}]}
 * A null upToAmount marks the last (unbounded) tier.
 */
public class InterestRateServiceRate {

    private String accountType;
    private List<Tier> tiers;

    public InterestRateServiceRate() {
    }

    public InterestRateServiceRate(String accountType, List<Tier> tiers) {
        this.accountType = accountType;
        this.tiers = tiers;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    public void setTiers(List<Tier> tiers) {
        this.tiers = tiers;
    }

    public static class Tier {
        private BigDecimal upToAmount;
        private BigDecimal ratePercentage;

        public Tier() {
        }

        public Tier(BigDecimal upToAmount, BigDecimal ratePercentage) {
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
}
