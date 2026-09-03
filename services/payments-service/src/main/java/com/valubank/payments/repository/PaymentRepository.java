package com.valubank.payments.repository;

import com.valubank.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByFromAccountIdOrderByTimestampDesc(Long fromAccountId);
}
