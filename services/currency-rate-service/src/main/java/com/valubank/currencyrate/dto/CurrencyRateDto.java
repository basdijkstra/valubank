package com.valubank.currencyrate.dto;

import java.math.BigDecimal;

/**
 * GET /api/currency-rates/{from}/{to} -> {"from":"EUR","to":"USD","rate":1.08}
 *
 * Multiplying an amount in `from` by `rate` gives the equivalent amount in `to`.
 */
public record CurrencyRateDto(String from, String to, BigDecimal rate) {
}
