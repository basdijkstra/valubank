package com.valubank.currencyrate.exception;

/**
 * Thrown when no exchange rate is configured for a requested currency pair.
 */
public class CurrencyRateNotFoundException extends RuntimeException {

    public CurrencyRateNotFoundException(String from, String to) {
        super("No exchange rate configured for " + from + " -> " + to);
    }
}
