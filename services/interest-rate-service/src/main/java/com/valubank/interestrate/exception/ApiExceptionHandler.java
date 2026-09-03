package com.valubank.interestrate.exception;

import com.valubank.interestrate.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates known service exceptions into clean JSON error responses
 * instead of Spring's default whitelabel error page.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InterestRateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(InterestRateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
