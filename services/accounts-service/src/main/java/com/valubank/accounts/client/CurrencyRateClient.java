package com.valubank.accounts.client;

import com.valubank.accounts.dto.CurrencyRateServiceRate;
import com.valubank.accounts.exception.CurrencyRateServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP client wrapping calls to the separate Currency Rate Service.
 * Kept as its own class (rather than inlined in the service) so it can be
 * mocked in unit tests of the balance-mutation / conversion logic.
 */
@Component
public class CurrencyRateClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CurrencyRateClient(RestTemplate restTemplate,
                               @Value("${valubank.currency-rate-service.url:http://localhost:8085}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public CurrencyRateServiceRate getRate(String from, String to) {
        try {
            String url = baseUrl + "/api/currency-rates/" + from + "/" + to;
            return restTemplate.getForObject(url, CurrencyRateServiceRate.class);
        } catch (RestClientException ex) {
            throw new CurrencyRateServiceException("Failed to retrieve exchange rate " + from + " -> " + to, ex);
        }
    }
}
