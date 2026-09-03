package com.valubank.interestrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ValuBank Interest Rate / Configuration Service.
 *
 * This is a deliberately small service: it exists mainly to give the
 * (separately built) Accounts Service a real external dependency that
 * workshop participants can practice isolating / mocking when unit
 * testing interest calculation logic.
 */
@SpringBootApplication
public class InterestRateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterestRateServiceApplication.class, args);
    }
}
