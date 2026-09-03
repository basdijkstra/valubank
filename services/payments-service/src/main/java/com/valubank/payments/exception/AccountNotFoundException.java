package com.valubank.payments.exception;

/**
 * Thrown when the Accounts Service returns 404 for a given account id, or
 * when the source account simply cannot be verified at all.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
