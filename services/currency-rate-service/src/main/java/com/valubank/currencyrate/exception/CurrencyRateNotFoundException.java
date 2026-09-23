package com.valubank.currencyrate.exception;

public class CurrencyRateNotFoundException extends RuntimeException {

    public CurrencyRateNotFoundException(String from, String to) {
        super("No exchange rate configured for " + from + " -> " + to);
    }
}
