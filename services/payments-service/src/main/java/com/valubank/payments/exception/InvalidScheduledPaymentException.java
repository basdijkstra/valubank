package com.valubank.payments.exception;

/**
 * Thrown when a scheduled payment request breaks a business rule that can
 * be checked up front (execution date not in the future, non-positive
 * amount, malformed IBAN, ...). Mapped to 400.
 */
public class InvalidScheduledPaymentException extends RuntimeException {

    public InvalidScheduledPaymentException(String message) {
        super(message);
    }
}
