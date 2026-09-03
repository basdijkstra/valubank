package com.valubank.interestrate.dto;

/**
 * Simple error body, e.g. {"error":"No interest rate configured for account type: CHECKING"}
 */
public class ErrorResponse {

    private String error;

    public ErrorResponse() {
    }

    public ErrorResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
