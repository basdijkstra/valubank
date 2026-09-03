package com.valubank.payments.exception;

import com.valubank.payments.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Keeps API responses on the shapes the spec/frontend expect instead of
 * Spring's default error pages. Simple in-app @ControllerAdvice - this is
 * a workshop demo, not production hardening.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SourceAccountUnverifiableException.class)
    public ResponseEntity<ErrorResponse> handleSourceAccountUnverifiable(SourceAccountUnverifiableException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Could not verify source account"));
    }
}
