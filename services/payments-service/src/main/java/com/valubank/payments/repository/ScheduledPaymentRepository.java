package com.valubank.payments.repository;

import com.valubank.payments.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    List<ScheduledPayment> findByCustomerIdOrderByExecutionDateAscIdAsc(Long customerId);

    List<ScheduledPayment> findByStatusAndExecutionDateLessThanEqualOrderByIdAsc(String status, LocalDate executionDate);
}
