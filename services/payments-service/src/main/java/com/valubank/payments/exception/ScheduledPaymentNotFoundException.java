package com.valubank.payments.exception;

/**
 * Thrown when a scheduled payment does not exist, or does not belong to the
 * customer asking for it (deliberately indistinguishable). Mapped to 404.
 */
public class ScheduledPaymentNotFoundException extends RuntimeException {

    public ScheduledPaymentNotFoundException(String message) {
        super(message);
    }
}
