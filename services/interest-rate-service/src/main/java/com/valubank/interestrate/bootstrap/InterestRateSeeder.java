package com.valubank.interestrate.bootstrap;

import com.valubank.interestrate.entity.InterestRateConfig;
import com.valubank.interestrate.repository.InterestRateConfigRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the Configuration DB with default interest rates on startup so the
 * service is immediately useful in the workshop without manual setup.
 */
@Component
public class InterestRateSeeder implements ApplicationRunner {

    private final InterestRateConfigRepository repository;

    public InterestRateSeeder(InterestRateConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        repository.save(new InterestRateConfig("CHECKING", BigDecimal.valueOf(0.1)));
        repository.save(new InterestRateConfig("SAVINGS", BigDecimal.valueOf(1.5)));
    }
}
