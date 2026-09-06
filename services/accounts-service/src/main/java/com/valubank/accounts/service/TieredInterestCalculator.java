package com.valubank.accounts.service;

import com.valubank.accounts.dto.InterestRateServiceRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Applies each tier's rate only to the portion of the balance that falls within it -
 * e.g. tiers [{upTo:10000, rate:1.5}, {upTo:null, rate:1.0}] on a balance of 12000
 * earns 1.5% on 10000 and 1.0% on the remaining 2000. Works for any number of tiers,
 * so it needs no per-account-type branching.
 */
public class TieredInterestCalculator {

    public BigDecimal calculateInterest(List<InterestRateServiceRate.Tier> tiers, BigDecimal balance) {
        BigDecimal previousThreshold = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (InterestRateServiceRate.Tier tier : tiers) {
            BigDecimal tierCap = tier.getUpToAmount() == null ? balance : tier.getUpToAmount();
            BigDecimal tierBalance = balance.min(tierCap).subtract(previousThreshold).max(BigDecimal.ZERO);

            totalInterest = totalInterest.add(tierBalance
                    .multiply(tier.getRatePercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

            previousThreshold = tierCap;
            if (balance.compareTo(previousThreshold) <= 0) {
                break;
            }
        }

        return totalInterest;
    }
}
