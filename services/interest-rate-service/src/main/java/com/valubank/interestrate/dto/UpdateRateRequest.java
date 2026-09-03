package com.valubank.interestrate.dto;

import java.math.BigDecimal;

/**
 * Request body for PUT /api/interest-rates/{accountType}, e.g.
 * {"ratePercentage": 0.2}
 */
public class UpdateRateRequest {

    private BigDecimal ratePercentage;

    public UpdateRateRequest() {
    }

    public UpdateRateRequest(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }

    public BigDecimal getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }
}
