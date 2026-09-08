package com.valubank.accounts.exception;

public class CurrencyRateServiceException extends RuntimeException {

    public CurrencyRateServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
