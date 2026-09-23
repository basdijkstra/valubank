package com.valubank.accounts.client;

import com.valubank.accounts.dto.CurrencyRateServiceRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP client wrapping calls to the separate Currency Rate Service.
 * Kept as its own class (rather than inlined in the service) so it can be
 * mocked in unit tests of the conversion logic.
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
        String url = baseUrl + "/api/currency-rates/" + from + "/" + to;
        return restTemplate.getForObject(url, CurrencyRateServiceRate.class);
    }
}
