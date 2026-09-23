package com.valubank.currencyrate.dto;

/**
 * Simple error body returned for unknown currency pairs, e.g. {"error":"..."}.
 */
public record ErrorResponse(String error) {
}
