package com.valubank.interestrate.exception;

/**
 * Thrown when no interest rate configuration exists for a requested account type.
 */
public class InterestRateNotFoundException extends RuntimeException {

    private final String accountType;

    public InterestRateNotFoundException(String accountType) {
        super("No interest rate configured for account type: " + accountType);
        this.accountType = accountType;
    }

    public String getAccountType() {
        return accountType;
    }
}
