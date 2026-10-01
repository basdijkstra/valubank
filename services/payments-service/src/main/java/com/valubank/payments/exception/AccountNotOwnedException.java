package com.valubank.payments.exception;

/**
 * Thrown when a customer tries to schedule a payment from an account that
 * belongs to someone else. Mapped to 403.
 */
public class AccountNotOwnedException extends RuntimeException {

    public AccountNotOwnedException(String message) {
        super(message);
    }
}
