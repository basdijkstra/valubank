package com.valubank.payments;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Json;
import com.valubank.payments.dto.AccountDto;
import com.valubank.payments.dto.ErrorResponse;
import com.valubank.payments.dto.FraudCheckResponse;
import com.valubank.payments.dto.PaymentRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Disabled 
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiWireMockTest {

    private static final WireMockServer ACCOUNTS_SERVICE = new WireMockServer(options().dynamicPort());
    private static final WireMockServer FRAUD_SERVICE = new WireMockServer(options().dynamicPort());

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startWireMockServers() {
        ACCOUNTS_SERVICE.start();
        FRAUD_SERVICE.start();
    }

    @AfterAll
    static void stopWireMockServers() {
        ACCOUNTS_SERVICE.stop();
        FRAUD_SERVICE.stop();
    }

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("valubank.accounts-service.url", () -> "http://localhost:" + ACCOUNTS_SERVICE.port());
        registry.add("valubank.fraud-service.url", () -> "http://localhost:" + FRAUD_SERVICE.port());
    }

    @Test
    void paymentThatExceedsSourceAccountBalanceIsRejectedForInsufficientFunds() {

        AccountDto account = new AccountDto();
        account.setId(3L);
        account.setCustomerId(2L);
        account.setIban("NL01VALU0000000003");
        account.setAccountType("CHECKING");
        account.setBalance(new BigDecimal("500.00"));
        account.setCurrency("EUR");

        ACCOUNTS_SERVICE.stubFor(get(urlEqualTo("/api/accounts/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Json.write(account))));

        /**
         * TODO: Use WireMock to stub the /api/accounts/3/balance-mutations endpoint on the accounts-service
         * to return a 422 Unprocessable Entity response with a JSON body containing
         * an "error" field with the value "Insufficient funds" when a POST request is
         * made to that endpoint.
         */


        /**
         * TODO: Use WireMock to stub the /api/fraud-checks endpoint on the fraud-service to return a 200 OK response
         * with a JSON body containing an "approved" field with the value true
         * when a POST request is made to that endpoint.
         * 
         * This is necessary because the payments-service will call the fraud-service to check if the payment
         * is approved before proceeding with the debit, and we want to ensure that the fraud check passes,
         * so that we can test the insufficient funds scenario without being blocked by a fraud check failure.
         */


        /**
         * TODO: Run the test and observe that it fails, because the accounts-service currently
         * returns 422 Unprocessable Entity for insufficient funds, while the payments-service
         * still expects 409 Conflict. This results in the status to be FAILED, not REJECTED.
         * 
         * To make sure your mock definitions are OK, change the status code returned
         * by WireMock for the POST call to /api/accounts/3/balance-mutations to 409.
         * This should make the test pass.
         */

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setFromAccountId(3L);
        paymentRequest.setToAccountIban("NL39RABO0300065264");
        paymentRequest.setToAccountName("J. de Boer");
        paymentRequest.setAmount(new BigDecimal("600.00"));
        paymentRequest.setCurrency("EUR");
        paymentRequest.setDescription("Rent");

        given()
                .port(port)
                .contentType("application/json")
                .body(paymentRequest)
        .when()
                .post("/api/payments")
        .then()
                .log().all()
                .statusCode(201)
                .body("status", equalTo("REJECTED"))
                .body("reason", equalTo("Insufficient funds"));
    }
}
