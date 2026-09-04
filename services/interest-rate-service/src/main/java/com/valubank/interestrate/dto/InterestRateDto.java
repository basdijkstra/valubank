package com.valubank.interestrate.dto;

import java.util.List;

/**
 * Response shape returned by the interest-rate endpoints: the ordered tier
 * schedule for one account type, e.g.
 * {"accountType":"SAVINGS","tiers":[{"upToAmount":10000.00,"ratePercentage":1.5},
 *  {"upToAmount":null,"ratePercentage":1.0}]}
 */
public class InterestRateDto {

    private String accountType;
    private List<InterestRateTierDto> tiers;

    public InterestRateDto() {
    }

    public InterestRateDto(String accountType, List<InterestRateTierDto> tiers) {
        this.accountType = accountType;
        this.tiers = tiers;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public List<InterestRateTierDto> getTiers() {
        return tiers;
    }

    public void setTiers(List<InterestRateTierDto> tiers) {
        this.tiers = tiers;
    }
}
