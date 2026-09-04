package com.valubank.interestrate.bootstrap;

import com.valubank.interestrate.entity.InterestRateTier;
import com.valubank.interestrate.repository.InterestRateTierRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the Configuration DB with default interest rate schedules on startup
 * so the service is immediately useful in the workshop without manual setup.
 */
@Component
public class InterestRateSeeder implements ApplicationRunner {

    private final InterestRateTierRepository repository;

    public InterestRateSeeder(InterestRateTierRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        // CHECKING: single, unbounded tier.
        repository.save(new InterestRateTier("CHECKING", 0, null, BigDecimal.valueOf(0.1)));

        // SAVINGS: 1.5% up to and including 10,000, 1.0% on the portion above.
        repository.save(new InterestRateTier("SAVINGS", 0, BigDecimal.valueOf(10_000), BigDecimal.valueOf(1.5)));
        repository.save(new InterestRateTier("SAVINGS", 1, null, BigDecimal.valueOf(1.0)));
    }
}
