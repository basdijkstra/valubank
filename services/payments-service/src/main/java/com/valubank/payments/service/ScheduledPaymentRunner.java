package com.valubank.payments.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically executes scheduled payments that have become due. Runs on
 * startup too, to catch up on anything that fell due while the service was down.
 */
@Component
public class ScheduledPaymentRunner {

    private final ScheduledPaymentService scheduledPaymentService;

    public ScheduledPaymentRunner(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        scheduledPaymentService.executeDuePayments();
    }

    @Scheduled(cron = "${valubank.scheduled-payments.cron}")
    public void runDuePayments() {
        scheduledPaymentService.executeDuePayments();
    }
}
