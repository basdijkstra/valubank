package com.valubank.payments.exception;

/**
 * Thrown when a downstream dependency (Accounts Service or Fraud Service)
 * could not be reached at all, or responded with something other than the
 * specific status codes this service knows how to interpret.
 */
public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String message) {
        super(message);
    }

    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
