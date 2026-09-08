package com.valubank.payments;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.PactSpecVersion;

import com.valubank.payments.client.AccountsServiceClient;
import com.valubank.payments.exception.InsufficientFundsException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "accounts-service", pactVersion = PactSpecVersion.V3)
@Disabled 
class AccountsServiceClientPactTestAnswer {

    @Pact(consumer = "payments-service")
    public RequestResponsePact debitRejectedWithInsufficientFunds(PactDslWithProvider builder) {

        DslPart requestBody = newJsonBody(o -> {
            o.stringType("type", "DEBIT");
            o.decimalType("amount", 200.00);
            o.stringType("reason", "Payment to NL39RABO0300065264");
        }).build();

        /**
         * TODO: Create a new DslPart for the response body that contains a single field 'error' with a string value.
         */

        DslPart responseBody = LambdaDsl.newJsonBody(o -> 
            o.stringType("error", "an example error message")
        ).build();

        /**
         * TODO: Use the Pact DSL to define a contract for a POST request to /api/accounts/1/balance-mutations
         * with a JSON body containing a debit of 200.00, which exceeds the account's balance of 50.00,
         * and returns a 409 Conflict response with a JSON body containing an 'error' field with a string value.
         * 
         * Properties to be defined for the request
         * - given: "account 1 exists with balance 50.00" (the provider state)
         * - uponReceiving: "a debit of 200.00 against account 1, which exceeds its balance" (the description of the interaction)
         * - path: "/api/accounts/1/balance-mutations" (the request path)
         * - method: "POST" (the request method)
         * - headers: "Content-Type" = "application/json" (the request header)
         * - request body: the request body predefined above
         * 
         * Properties to be defined for the response
         * - expected response status: 409
         * - expected response body: the response body you defined above
         * 
         * See https://github.com/basdijkstra/introduction-to-contract-testing/blob/main/customer-consumer/src/test/java/customer/AddressServiceGetContractTest.java
         * for an example of using the Pact DSL to define a contract.
         */

        return builder
                .given("account 1 exists with balance 50.00")
                .uponReceiving("a debit of 200.00 against account 1, which exceeds its balance")
                .path("/api/accounts/1/balance-mutations")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(requestBody)
                .willRespondWith()
                .status(409)
                .body(responseBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "debitRejectedWithInsufficientFunds")
    void debitThatExceedsBalanceIsReportedAsInsufficientFunds(MockServer mockServer) {

        AccountsServiceClient client = new AccountsServiceClient(new RestTemplate(), mockServer.getUrl());

        /**
         * TODO: Call the debit method of the AccountsServiceClient to debit 200.00 from account 1,
         * which has a balance of 50.00. Assert that an InsufficientFundsException is thrown.
         */

        assertThrows(InsufficientFundsException.class,
                () -> client.debit(1L, new BigDecimal("200.00"), "Payment to NL39RABO0300065264"));
    }
}
