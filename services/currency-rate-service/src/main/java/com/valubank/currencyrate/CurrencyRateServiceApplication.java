package com.valubank.currencyrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ValuBank Currency Rate Service.
 *
 * <p>This service exposes a fixed, in-memory exchange rate table. It has no
 * database and no external dependencies - it is called server-to-server by
 * the Accounts Service when a payment mutation arrives in a currency other
 * than the account's own.</p>
 */
@SpringBootApplication
public class CurrencyRateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyRateServiceApplication.class, args);
    }
}
