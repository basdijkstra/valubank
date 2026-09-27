package com.valubank.assistant.exception;

import com.valubank.assistant.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Keeps API responses on the shapes the frontend expects instead of Spring's
 * default error pages. Simple in-app @ControllerAdvice - this is a workshop
 * demo, not production hardening.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AssistantException.class)
    public ResponseEntity<ErrorResponse> handleAssistantError(AssistantException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(e.getMessage()));
    }
}
