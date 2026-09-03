package com.valubank.accounts.client;

import com.valubank.accounts.dto.InterestRateServiceRate;
import com.valubank.accounts.exception.InterestRateServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP client wrapping calls to the separate Interest Rate / Configuration Service.
 * Kept as its own class (rather than inlined in the controller) so it can be mocked
 * in unit tests of the interest-lookup logic.
 */
@Component
public class InterestRateClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InterestRateClient(RestTemplate restTemplate,
                               @Value("${valubank.interest-rate-service.url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public InterestRateServiceRate getRateForAccountType(String accountType) {
        try {
            String url = baseUrl + "/api/interest-rates/" + accountType;
            return restTemplate.getForObject(url, InterestRateServiceRate.class);
        } catch (RestClientException ex) {
            throw new InterestRateServiceException("Failed to retrieve interest rate for account type " + accountType, ex);
        }
    }
}
