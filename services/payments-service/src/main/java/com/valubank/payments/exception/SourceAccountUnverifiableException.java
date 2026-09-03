package com.valubank.payments.exception;

/**
 * Thrown when the source account for a payment could not be verified at
 * all (not found, or the Accounts Service call failed outright). Per spec
 * this must short-circuit the whole payment attempt with a 502 - no
 * Payment record is saved in this case.
 */
public class SourceAccountUnverifiableException extends RuntimeException {

    public SourceAccountUnverifiableException(String message) {
        super(message);
    }
}
