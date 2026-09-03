package com.valubank.payments.exception;

/**
 * Thrown when the Accounts Service rejects a debit with 409 (insufficient
 * funds) - a race-condition edge case where the balance changed between our
 * initial read and the actual debit attempt.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
