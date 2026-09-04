package com.valubank.interestrate.repository;

import com.valubank.interestrate.entity.InterestRateTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRateTierRepository extends JpaRepository<InterestRateTier, Long> {

    List<InterestRateTier> findByAccountTypeIgnoreCaseOrderByTierOrderAsc(String accountType);

    List<InterestRateTier> findAllByOrderByAccountTypeAscTierOrderAsc();

    void deleteByAccountTypeIgnoreCase(String accountType);
}
