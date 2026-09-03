package com.valubank.payments.controller;

import com.valubank.payments.dto.PaymentRequest;
import com.valubank.payments.entity.Payment;
import com.valubank.payments.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments")
    public ResponseEntity<Payment> createPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/api/accounts/{accountId}/payments")
    public ResponseEntity<List<Payment>> getPaymentsForAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(paymentService.getPaymentsForAccount(accountId));
    }
}
