package com.valubank.accounts.controller;

import com.valubank.accounts.dto.AccountDto;
import com.valubank.accounts.dto.BalanceMutationRequest;
import com.valubank.accounts.dto.InterestRateResponse;
import com.valubank.accounts.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/api/customers/{customerId}/accounts")
    public List<AccountDto> getAccountsForCustomer(@PathVariable Long customerId) {
        return accountService.getAccountsForCustomer(customerId);
    }

    @GetMapping("/api/accounts/{accountId}")
    public AccountDto getAccount(@PathVariable Long accountId) {
        return accountService.getAccount(accountId);
    }

    @GetMapping("/api/accounts/{accountId}/interest-rate")
    public InterestRateResponse getInterestRate(@PathVariable Long accountId) {
        return accountService.getInterestRate(accountId);
    }

    @PostMapping("/api/accounts/{accountId}/balance-mutations")
    public AccountDto applyBalanceMutation(@PathVariable Long accountId, @RequestBody BalanceMutationRequest request) {
        return accountService.applyBalanceMutation(accountId, request);
    }
}
