package com.valubank.payments.controller;

import com.valubank.payments.entity.ScheduledPayment;
import com.valubank.payments.service.ScheduledPaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEST-ONLY endpoints, so nobody has to wait for a scheduled payment's
 * execution date to see what happens. Only registered when
 * valubank.test-support.enabled=true - a real deployment would leave this off.
 */
@RestController
@ConditionalOnProperty(name = "valubank.test-support.enabled", havingValue = "true")
public class TestSupportController {

    private final ScheduledPaymentService scheduledPaymentService;

    public TestSupportController(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    /** Executes one scheduled payment right now, ignoring its execution date. */
    @PostMapping("/api/test-support/scheduled-payments/{id}/execute")
    public ResponseEntity<ScheduledPayment> executeNow(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledPaymentService.execute(id));
    }
}
