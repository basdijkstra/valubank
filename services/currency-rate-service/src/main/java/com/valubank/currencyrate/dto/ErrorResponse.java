package com.valubank.currencyrate.dto;

/**
 * Simple error body, e.g. {"error":"No exchange rate configured for EUR -> JPY"}
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
