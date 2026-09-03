package com.valubank.payments.client;

import com.valubank.payments.dto.FraudCheckRequest;
import com.valubank.payments.dto.FraudCheckResponse;
import com.valubank.payments.exception.DependencyUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Plain point-to-point HTTP client for the Fraud Service. Same story as
 * AccountsServiceClient: no shared contract, just a RestTemplate call
 * against the fixed JSON shape the Fraud Service team publishes.
 */
@Component
public class FraudServiceClient {

    private final RestTemplate restTemplate;
    private final String fraudServiceUrl;

    public FraudServiceClient(RestTemplate restTemplate,
                               @Value("${valubank.fraud-service.url}") String fraudServiceUrl) {
        this.restTemplate = restTemplate;
        this.fraudServiceUrl = fraudServiceUrl;
    }

    /**
     * POST {fraud-service.url}/api/fraud-checks
     *
     * @throws DependencyUnavailableException if the Fraud Service could not be reached or fails unexpectedly
     */
    public FraudCheckResponse checkFraud(Long fromAccountId, String toAccountIban, java.math.BigDecimal amount) {
        String url = fraudServiceUrl + "/api/fraud-checks";
        FraudCheckRequest request = new FraudCheckRequest(fromAccountId, toAccountIban, amount);
        try {
            FraudCheckResponse response = restTemplate.postForObject(url, new HttpEntity<>(request), FraudCheckResponse.class);
            if (response == null) {
                throw new DependencyUnavailableException("Fraud service returned an empty response");
            }
            return response;
        } catch (ResourceAccessException e) {
            throw new DependencyUnavailableException("Fraud service unavailable", e);
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Fraud service call failed", e);
        }
    }
}
