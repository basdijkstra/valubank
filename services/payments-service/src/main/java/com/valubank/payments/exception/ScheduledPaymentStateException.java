package com.valubank.payments.exception;

/**
 * Thrown when a scheduled payment can't be cancelled or executed because it
 * is no longer SCHEDULED (already executed, failed, or cancelled). Mapped to 409.
 */
public class ScheduledPaymentStateException extends RuntimeException {

    public ScheduledPaymentStateException(String message) {
        super(message);
    }
}
