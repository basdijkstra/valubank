package com.valubank.interestrate.repository;

import com.valubank.interestrate.entity.InterestRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterestRateConfigRepository extends JpaRepository<InterestRateConfig, Long> {

    Optional<InterestRateConfig> findByAccountTypeIgnoreCase(String accountType);
}
