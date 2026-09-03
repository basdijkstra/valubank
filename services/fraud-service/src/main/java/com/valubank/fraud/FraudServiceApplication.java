package com.valubank.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ValuBank Fraud Service.
 *
 * <p>This service performs simple, in-memory rule based fraud checks on
 * payments. It has no database and no external dependencies - it is called
 * server-to-server by the Payments Service.</p>
 */
@SpringBootApplication
public class FraudServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudServiceApplication.class, args);
    }
}
