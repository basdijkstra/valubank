package com.valubank.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ValuBank Assistant Service.
 *
 * <p>Exposes a conversational assistant that answers questions about a
 * customer's own accounts and payments, using Claude's tool-use to call
 * into the Accounts Service and Payments Service on the customer's
 * behalf.</p>
 */
@SpringBootApplication
public class AssistantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantServiceApplication.class, args);
    }
}
