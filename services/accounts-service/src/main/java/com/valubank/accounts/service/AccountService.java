package com.valubank.accounts.service;

import com.valubank.accounts.client.InterestRateClient;
import com.valubank.accounts.dto.AccountDto;
import com.valubank.accounts.dto.BalanceMutationRequest;
import com.valubank.accounts.dto.InterestRateResponse;
import com.valubank.accounts.dto.InterestRateServiceRate;
import com.valubank.accounts.entity.Account;
import com.valubank.accounts.exception.AccountNotFoundException;
import com.valubank.accounts.exception.InsufficientFundsException;
import com.valubank.accounts.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final InterestRateClient interestRateClient;

    public AccountService(AccountRepository accountRepository, InterestRateClient interestRateClient) {
        this.accountRepository = accountRepository;
        this.interestRateClient = interestRateClient;
    }

    public List<AccountDto> getAccountsForCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(AccountDto::from)
                .toList();
    }

    public AccountDto getAccount(Long accountId) {
        return AccountDto.from(findAccountOrThrow(accountId));
    }

    public InterestRateResponse getInterestRate(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        InterestRateServiceRate rate = interestRateClient.getRateForAccountType(account.getAccountType());
        return new InterestRateResponse(account.getId(), account.getAccountType(), rate.getRatePercentage());
    }

    public AccountDto applyBalanceMutation(Long accountId, BalanceMutationRequest request) {
        Account account = findAccountOrThrow(accountId);
        BigDecimal amount = request.getAmount();

        BigDecimal newBalance;
        if ("DEBIT".equalsIgnoreCase(request.getType())) {
            newBalance = account.getBalance().subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }
        } else {
            newBalance = account.getBalance().add(amount);
        }

        account.setBalance(newBalance);
        accountRepository.save(account);
        return AccountDto.from(account);
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }
}
