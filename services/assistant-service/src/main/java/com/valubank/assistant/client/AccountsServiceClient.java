package com.valubank.assistant.client;

import com.valubank.assistant.exception.ToolExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Plain point-to-point HTTP client for the Accounts Service, in the same
 * style as the Payments Service's client - no shared contract, just a
 * RestTemplate and raw JSON passed straight through as tool results.
 *
 * These calls are made with whatever customerId/accountId the model decides
 * to use - this client does not check that they belong to the customer the
 * assistant is currently serving. Scoping is only enforced by instructing
 * the model in the system prompt.
 */
@Component
public class AccountsServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AccountsServiceClient(RestTemplate restTemplate,
                                  @Value("${valubank.accounts-service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public String listAccountsForCustomer(Long customerId) {
        return get(baseUrl + "/api/customers/" + customerId + "/accounts");
    }

    public String getAccount(Long accountId) {
        return get(baseUrl + "/api/accounts/" + accountId);
    }

    public String getInterestRate(Long accountId) {
        return get(baseUrl + "/api/accounts/" + accountId + "/interest-rate");
    }

    private String get(String url) {
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ToolExecutionException("Not found");
        } catch (ResourceAccessException e) {
            throw new ToolExecutionException("Accounts service unavailable");
        } catch (RestClientException e) {
            throw new ToolExecutionException("Accounts service call failed");
        }
    }
}
