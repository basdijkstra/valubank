package com.valubank.currencyrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ValuBank Currency Rate Service.
 *
 * This is a deliberately small service: it exists to give the Accounts
 * Service a real external dependency for currency conversion, the same
 * way the Interest Rate Service does for interest calculation.
 */
@SpringBootApplication
public class CurrencyRateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyRateServiceApplication.class, args);
    }
}
