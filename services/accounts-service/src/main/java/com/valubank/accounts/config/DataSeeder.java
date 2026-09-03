package com.valubank.accounts.config;

import com.valubank.accounts.entity.Account;
import com.valubank.accounts.entity.Customer;
import com.valubank.accounts.repository.AccountRepository;
import com.valubank.accounts.repository.CustomerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds demo data on startup. Written in plain Java (instead of data.sql) so workshop
 * participants can easily read and tweak it.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public DataSeeder(CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Customer alice = customerRepository.save(new Customer("alice", "password123", "Alice Janssen"));
        Customer bob = customerRepository.save(new Customer("bob", "password123", "Bob de Vries"));

        accountRepository.save(new Account(alice.getId(), "NL01VALU0000000001", "CURRENT",
                new BigDecimal("2500.00"), "EUR"));
        accountRepository.save(new Account(alice.getId(), "NL01VALU0000000002", "SAVINGS",
                new BigDecimal("10000.00"), "EUR"));
        accountRepository.save(new Account(bob.getId(), "NL01VALU0000000003", "CURRENT",
                new BigDecimal("500.00"), "EUR"));
    }
}
