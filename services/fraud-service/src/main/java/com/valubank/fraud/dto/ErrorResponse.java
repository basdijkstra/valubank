package com.valubank.fraud.dto;

/**
 * Simple error body returned for malformed requests, e.g. {"error":"..."}.
 */
public record ErrorResponse(String error) {
}
