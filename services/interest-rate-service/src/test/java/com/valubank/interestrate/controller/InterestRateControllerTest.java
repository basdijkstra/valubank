package com.valubank.interestrate.controller;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;

// Spring Boot starts a real embedded server on a random free port for the test
// and tears it down afterwards - no manual start/stop of the service needed.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InterestRateControllerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @CsvSource({
            "SAVINGS, 1.5",
            "CHECKING, 0.1"
    })
    void getRateForAccountType(String accountType, float expectedRatePercentage) {
        RestAssured
                .when()
                    .get("/api/interest-rates/{accountType}", accountType)
                .then()
                    .statusCode(200)
                    .body("accountType", equalTo(accountType))
                    .body("ratePercentage", equalTo(expectedRatePercentage));
    }
}
