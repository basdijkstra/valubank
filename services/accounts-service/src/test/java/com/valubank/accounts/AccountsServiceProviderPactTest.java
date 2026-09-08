package com.valubank.accounts;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.valubank.accounts.entity.Account;
import com.valubank.accounts.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

/**
 * Verifies the pact recorded by payments-service (AccountsServiceClientPactTest) against a real,
 * running accounts-service - same controllers, same service layer, same H2 schema, just seeded
 * per interaction via the @State methods below. A run against this codebase's current behaviour
 * (422 on insufficient funds) fails the "debit rejected with insufficient funds" interaction,
 * because payments-service's contract still expects 409 - exactly the break this test exists to
 * catch before either service reaches a shared environment.
 */
@Provider("accounts-service")
@PactFolder("src/test/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountsServiceProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("account 1 exists with balance 50.00")
    void account1WithBalance50() {
        seedAccount1(new BigDecimal("50.00"));
    }

    @State("account 1 exists with balance 500.00")
    void account1WithBalance500() {
        seedAccount1(new BigDecimal("500.00"));
    }

    // TRUNCATE clears the rows but leaves the identity sequence wherever it was, so it must be
    // restarted explicitly - otherwise only the very first seeded account across the whole run
    // gets id 1, and every interaction after that seeds a row at id 2, 3, ... while the pact's
    // recorded requests all hardcode /api/accounts/1.
    private void seedAccount1(BigDecimal balance) {
        jdbcTemplate.execute("TRUNCATE TABLE accounts");
        jdbcTemplate.execute("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1");
        accountRepository.save(new Account(42L, "NL91ABNA0417164300", "SAVINGS", balance, "EUR"));
    }
}
