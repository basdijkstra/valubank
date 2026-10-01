package com.valubank.payments.controller;

import com.valubank.payments.dto.ScheduledPaymentRequest;
import com.valubank.payments.entity.ScheduledPayment;
import com.valubank.payments.service.ScheduledPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The customer is identified by a plain customerId (request body / query param),
 * in line with the rest of ValuBank's deliberately simplified auth.
 */
@RestController
public class ScheduledPaymentController {

    private final ScheduledPaymentService scheduledPaymentService;

    public ScheduledPaymentController(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    @PostMapping("/api/scheduled-payments")
    public ResponseEntity<ScheduledPayment> schedulePayment(@RequestBody ScheduledPaymentRequest request) {
        ScheduledPayment scheduledPayment = scheduledPaymentService.schedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduledPayment);
    }

    @GetMapping("/api/customers/{customerId}/scheduled-payments")
    public ResponseEntity<List<ScheduledPayment>> getScheduledPaymentsForCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(scheduledPaymentService.getScheduledPaymentsForCustomer(customerId));
    }

    @PostMapping("/api/scheduled-payments/{id}/cancel")
    public ResponseEntity<ScheduledPayment> cancelScheduledPayment(@PathVariable Long id,
                                                                   @RequestParam Long customerId) {
        return ResponseEntity.ok(scheduledPaymentService.cancel(id, customerId));
    }
}
