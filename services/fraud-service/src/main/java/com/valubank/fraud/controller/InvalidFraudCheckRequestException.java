package com.valubank.fraud.controller;

/**
 * Thrown when a fraud check request is missing required fields.
 */
public class InvalidFraudCheckRequestException extends RuntimeException {

    public InvalidFraudCheckRequestException(String message) {
        super(message);
    }
}
