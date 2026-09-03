package com.valubank.fraud.controller;

import com.valubank.fraud.dto.ErrorResponse;
import com.valubank.fraud.dto.FraudCheckRequest;
import com.valubank.fraud.dto.FraudCheckResponse;
import com.valubank.fraud.service.FraudRuleEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the fraud check API called by the Payments Service.
 */
@RestController
public class FraudCheckController {

    private final FraudRuleEngine fraudRuleEngine;

    public FraudCheckController(FraudRuleEngine fraudRuleEngine) {
        this.fraudRuleEngine = fraudRuleEngine;
    }

    @PostMapping("/api/fraud-checks")
    public FraudCheckResponse checkFraud(@RequestBody(required = false) FraudCheckRequest request) {
        validate(request);
        return fraudRuleEngine.evaluate(request);
    }

    private void validate(FraudCheckRequest request) {
        if (request == null) {
            throw new InvalidFraudCheckRequestException("Request body is required");
        }
        if (request.fromAccountId() == null) {
            throw new InvalidFraudCheckRequestException("fromAccountId is required");
        }
        if (request.toAccountIban() == null || request.toAccountIban().isBlank()) {
            throw new InvalidFraudCheckRequestException("toAccountIban is required");
        }
        if (request.amount() == null) {
            throw new InvalidFraudCheckRequestException("amount is required");
        }
    }

    @ExceptionHandler(InvalidFraudCheckRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidFraudCheckRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Malformed request body"));
    }
}
