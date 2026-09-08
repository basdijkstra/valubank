package com.valubank.payments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.valubank.payments.dto.PaymentRequest;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void paymentThatExceedsSourceAccountBalanceIsRejectedForInsufficientFunds() {

        /**
         * TODO: Create a new PaymentRequest instance with the following properties:
         * - fromAccountId: 3
         * - toAccountIban: "NL39RABO0300065264"
         * - toAccountName: "J. de Boer"
         * - amount: 600.00
         * - currency: "EUR"
         * - description: "Rent"
         */
        

        /**
         * TODO: Use RestAssured to POST the paymentRequest to /api/payments on this service's
         * real HTTP stack (port is injected above). The request should have content type "application/json"
         * and the paymentRequest should be serialized to JSON in the body. Use the port injected above.
         * 
         * Assert that the response has:
         * - status code 201 Created
         * - JSON body with "status" equal to "REJECTED"
         * - JSON body with "reason" equal to "Insufficient funds"
         * 
         * then().log().all() can be used to log the response body for debugging purposes.
         * 
         * The test should fail, because the accounts-service currently returns 422 Unprocessable Entity
         * for insufficient funds, while the payments-service contract still expects 409 Conflict.
         * This results in the status to be FAILED, not REJECTED.
         * 
         * You can find some examples of using RestAssured at https://github.com/basdijkstra/rest-assured-workshop/blob/main/src/test/java/answers/RestAssuredAnswers1Test.java
         */
        
    }
}
