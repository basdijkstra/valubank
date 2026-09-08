package com.valubank.currencyrate.dto;

import java.math.BigDecimal;

/**
 * Response shape returned by the currency-rate endpoint, e.g.
 * {"from":"EUR","to":"USD","rate":1.08}
 *
 * "rate" means: 1 unit of "from" is worth "rate" units of "to".
 */
public class CurrencyRateDto {

    private String from;
    private String to;
    private BigDecimal rate;

    public CurrencyRateDto() {
    }

    public CurrencyRateDto(String from, String to, BigDecimal rate) {
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
