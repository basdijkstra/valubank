package com.valubank.assistant.client;

import com.valubank.assistant.exception.ToolExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Plain point-to-point HTTP client for the Payments Service. Same scoping
 * caveat as {@link AccountsServiceClient}: forwards whatever accountId the
 * model asks for, with no ownership check against the current customer.
 */
@Component
public class PaymentsServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentsServiceClient(RestTemplate restTemplate,
                                  @Value("${valubank.payments-service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public String listPaymentsForAccount(Long accountId) {
        return get(baseUrl + "/api/accounts/" + accountId + "/payments");
    }

    private String get(String url) {
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ToolExecutionException("Not found");
        } catch (ResourceAccessException e) {
            throw new ToolExecutionException("Payments service unavailable");
        } catch (RestClientException e) {
            throw new ToolExecutionException("Payments service call failed");
        }
    }
}
