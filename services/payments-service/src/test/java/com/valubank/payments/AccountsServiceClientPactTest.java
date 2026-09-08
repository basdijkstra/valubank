package com.valubank.payments;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.PactSpecVersion;

import com.valubank.payments.client.AccountsServiceClient;
import com.valubank.payments.dto.AccountDto;
import com.valubank.payments.exception.InsufficientFundsException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "accounts-service", pactVersion = PactSpecVersion.V3)
@Disabled 
class AccountsServiceClientPactTest {

    @Pact(consumer = "payments-service")
    public RequestResponsePact debitRejectedWithInsufficientFunds(PactDslWithProvider builder) {
        return builder
                .given("account 1 exists with balance 50.00")
                .uponReceiving("a debit of 200.00 against account 1, which exceeds its balance")
                .path("/api/accounts/1/balance-mutations")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(newJsonBody(o -> {
                    o.stringType("type", "DEBIT");
                    o.decimalType("amount", 200.00);
                    o.stringType("reason", "Payment to NL39RABO0300065264");
                }).build())
                .willRespondWith()
                .status(409)
                .headers(Map.of("Content-Type", "application/json"))
                .body(newJsonBody(o -> o.stringType("error", "Insufficient funds")).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "debitRejectedWithInsufficientFunds")
    void debitThatExceedsBalanceIsReportedAsInsufficientFunds(MockServer mockServer) {
        AccountsServiceClient client = clientFor(mockServer);

        assertThrows(InsufficientFundsException.class,
                () -> client.debit(1L, new BigDecimal("200.00"), "Payment to NL39RABO0300065264"));
    }

    @Pact(consumer = "payments-service")
    public RequestResponsePact debitSucceeds(PactDslWithProvider builder) {
        return builder
                .given("account 1 exists with balance 500.00")
                .uponReceiving("a debit of 100.00 against account 1, which it can cover")
                .path("/api/accounts/1/balance-mutations")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(newJsonBody(o -> {
                    o.stringType("type", "DEBIT");
                    o.decimalType("amount", 100.00);
                    o.stringType("reason", "Payment to NL39RABO0300065264");
                }).build())
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(newJsonBody(o -> {
                    o.numberType("id", 1);
                    o.numberType("customerId", 42);
                    o.stringType("iban", "NL91ABNA0417164300");
                    o.stringType("accountType", "SAVINGS");
                    o.decimalType("balance", 400.00);
                    o.stringType("currency", "EUR");
                }).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "debitSucceeds")
    void debitThatFitsWithinBalanceReturnsTheUpdatedAccount(MockServer mockServer) {
        AccountsServiceClient client = clientFor(mockServer);

        AccountDto result = client.debit(1L, new BigDecimal("100.00"), "Payment to NL39RABO0300065264");

        assertEquals(new BigDecimal("400.0"), result.getBalance());
    }

    @Pact(consumer = "payments-service")
    public RequestResponsePact accountExists(PactDslWithProvider builder) {
        return builder
                .given("account 1 exists with balance 500.00")
                .uponReceiving("a request for account 1")
                .path("/api/accounts/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(newJsonBody(o -> {
                    o.numberType("id", 1);
                    o.numberType("customerId", 42);
                    o.stringType("iban", "NL91ABNA0417164300");
                    o.stringType("accountType", "SAVINGS");
                    o.decimalType("balance", 500.00);
                    o.stringType("currency", "EUR");
                }).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "accountExists")
    void getAccountReturnsTheAccountDetails(MockServer mockServer) {
        AccountsServiceClient client = clientFor(mockServer);

        AccountDto result = client.getAccount(1L);

        assertEquals(new BigDecimal("500.0"), result.getBalance());
        assertEquals("EUR", result.getCurrency());
    }

    private AccountsServiceClient clientFor(MockServer mockServer) {
        return new AccountsServiceClient(new RestTemplate(), mockServer.getUrl());
    }
}
