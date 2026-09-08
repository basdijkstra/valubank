package com.valubank.currencyrate.service;

import com.valubank.currencyrate.dto.CurrencyRateDto;
import com.valubank.currencyrate.exception.CurrencyRateNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Fixed, in-memory exchange rate table. No persistence and no admin update
 * endpoint on purpose: rates are static workshop fixtures, not something
 * this service manages the lifecycle of.
 *
 * Each direction is stored explicitly (rather than storing one side and
 * inverting it on lookup) so a EUR->USD and USD->EUR round trip isn't
 * silently assumed to be perfectly reciprocal, matching how real spot
 * rates work.
 */
@Service
public class CurrencyRateService {

    private static final Map<String, BigDecimal> RATES = Map.of(
            key("EUR", "USD"), BigDecimal.valueOf(1.08),
            key("USD", "EUR"), BigDecimal.valueOf(0.93),
            key("EUR", "GBP"), BigDecimal.valueOf(0.86),
            key("GBP", "EUR"), BigDecimal.valueOf(1.16),
            key("USD", "GBP"), BigDecimal.valueOf(0.79),
            key("GBP", "USD"), BigDecimal.valueOf(1.27)
    );

    public CurrencyRateDto getRate(String from, String to) {
        String normalizedFrom = from.toUpperCase();
        String normalizedTo = to.toUpperCase();

        if (normalizedFrom.equals(normalizedTo)) {
            return new CurrencyRateDto(normalizedFrom, normalizedTo, BigDecimal.ONE);
        }

        BigDecimal rate = RATES.get(key(normalizedFrom, normalizedTo));
        if (rate == null) {
            throw new CurrencyRateNotFoundException(normalizedFrom, normalizedTo);
        }
        return new CurrencyRateDto(normalizedFrom, normalizedTo, rate);
    }

    public List<CurrencyRateDto> getAllRates() {
        return RATES.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("_");
                    return new CurrencyRateDto(parts[0], parts[1], entry.getValue());
                })
                .toList();
    }

    private static String key(String from, String to) {
        return from + "_" + to;
    }
}
