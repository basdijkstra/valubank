package com.valubank.payments.dto;

/**
 * Simple {"error": "..."} response shape used for error responses on this
 * service's own API (e.g. 502 when the source account can't be verified).
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
