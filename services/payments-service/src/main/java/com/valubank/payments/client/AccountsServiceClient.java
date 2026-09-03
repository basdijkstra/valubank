package com.valubank.payments.client;

import com.valubank.payments.dto.AccountDto;
import com.valubank.payments.dto.BalanceMutationRequest;
import com.valubank.payments.exception.AccountNotFoundException;
import com.valubank.payments.exception.DependencyUnavailableException;
import com.valubank.payments.exception.InsufficientFundsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Plain point-to-point HTTP client for the Accounts Service. There is no
 * shared client library or generated OpenAPI client between Payments and
 * Accounts - just a RestTemplate and DTOs local to this project, matching
 * the fixed contract the two teams agreed on.
 */
@Component
public class AccountsServiceClient {

    private final RestTemplate restTemplate;
    private final String accountsServiceUrl;

    public AccountsServiceClient(RestTemplate restTemplate,
                                  @Value("${valubank.accounts-service.url}") String accountsServiceUrl) {
        this.restTemplate = restTemplate;
        this.accountsServiceUrl = accountsServiceUrl;
    }

    /**
     * GET {accounts-service.url}/api/accounts/{accountId}
     *
     * @throws AccountNotFoundException        if the account does not exist (404)
     * @throws DependencyUnavailableException  if the Accounts Service could not be reached
     */
    public AccountDto getAccount(Long accountId) {
        String url = accountsServiceUrl + "/api/accounts/" + accountId;
        try {
            AccountDto account = restTemplate.getForObject(url, AccountDto.class);
            if (account == null) {
                throw new AccountNotFoundException("Account " + accountId + " not found");
            }
            return account;
        } catch (HttpClientErrorException.NotFound e) {
            throw new AccountNotFoundException("Account " + accountId + " not found");
        } catch (ResourceAccessException e) {
            throw new DependencyUnavailableException("Accounts service unavailable", e);
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Accounts service call failed", e);
        }
    }

    /**
     * POST {accounts-service.url}/api/accounts/{accountId}/balance-mutations
     *
     * @throws InsufficientFundsException     if the Accounts Service rejects the debit with 409
     * @throws DependencyUnavailableException if the Accounts Service could not be reached or fails unexpectedly
     */
    public AccountDto debit(Long accountId, BigDecimal amount, String reason) {
        String url = accountsServiceUrl + "/api/accounts/" + accountId + "/balance-mutations";
        BalanceMutationRequest request = new BalanceMutationRequest("DEBIT", amount, reason);
        try {
            return restTemplate.postForObject(url, new HttpEntity<>(request), AccountDto.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientFundsException("Insufficient funds");
        } catch (ResourceAccessException e) {
            throw new DependencyUnavailableException("Accounts service unavailable", e);
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Accounts service call failed", e);
        }
    }
}
