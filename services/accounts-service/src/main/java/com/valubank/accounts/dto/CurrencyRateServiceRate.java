package com.valubank.accounts.dto;

import java.math.BigDecimal;

/**
 * Maps the raw JSON response returned by the separate Currency Rate Service:
 * GET http://.../api/currency-rates/{from}/{to} -> {"from":"EUR","to":"USD","rate":1.08}
 */
public class CurrencyRateServiceRate {

    private String from;
    private String to;
    private BigDecimal rate;

    public CurrencyRateServiceRate() {
    }

    public CurrencyRateServiceRate(String from, String to, BigDecimal rate) {
        this.from = from;
        this.to = to;
        this.rate = rate;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
