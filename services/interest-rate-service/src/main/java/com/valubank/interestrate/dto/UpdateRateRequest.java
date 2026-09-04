package com.valubank.interestrate.dto;

import java.util.List;

/**
 * Request body for PUT /api/interest-rates/{accountType}: replaces the full
 * tier schedule for that account type, e.g.
 * {"tiers":[{"upToAmount":10000.00,"ratePercentage":1.5},{"ratePercentage":1.0}]}
 */
public class UpdateRateRequest {

    private List<InterestRateTierDto> tiers;

    public UpdateRateRequest() {
    }

    public UpdateRateRequest(List<InterestRateTierDto> tiers) {
        this.tiers = tiers;
    }

    public List<InterestRateTierDto> getTiers() {
        return tiers;
    }

    public void setTiers(List<InterestRateTierDto> tiers) {
        this.tiers = tiers;
    }
}
