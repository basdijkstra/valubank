package com.valubank.accounts.service;

import com.valubank.accounts.client.InterestRateClient;
import com.valubank.accounts.dto.AccountDto;
import com.valubank.accounts.dto.AdminAccountDto;
import com.valubank.accounts.dto.BalanceMutationRequest;
import com.valubank.accounts.dto.InterestApplicationResponse;
import com.valubank.accounts.dto.InterestRateResponse;
import com.valubank.accounts.dto.InterestRateServiceRate;
import com.valubank.accounts.entity.Account;
import com.valubank.accounts.entity.Customer;
import com.valubank.accounts.exception.AccountNotFoundException;
import com.valubank.accounts.exception.InsufficientFundsException;
import com.valubank.accounts.repository.AccountRepository;
import com.valubank.accounts.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final InterestRateClient interestRateClient;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository,
                           InterestRateClient interestRateClient) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
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

    // Admin view - every account across all customers, with owner identity attached.
    public List<AdminAccountDto> getAllAccountsWithOwners() {
        return accountRepository.findAll()
                .stream()
                .map(account -> {
                    Customer owner = customerRepository.findById(account.getCustomerId())
                            .orElseThrow(() -> new AccountNotFoundException(
                                    "Owning customer not found for account " + account.getId()));
                    return AdminAccountDto.from(account, owner);
                })
                .toList();
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

    // Calculates interest on the current balance using the applicable rate from the
    // Interest Rate / Configuration Service, and credits it to the account.
    public InterestApplicationResponse applyInterest(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        InterestRateServiceRate rate = interestRateClient.getRateForAccountType(account.getAccountType());

        BigDecimal ratePercentage = BigDecimal.valueOf(rate.getRatePercentage());
        BigDecimal previousBalance = account.getBalance();
        BigDecimal interestAmount = previousBalance
                .multiply(ratePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal newBalance = previousBalance.add(interestAmount);

        account.setBalance(newBalance);
        accountRepository.save(account);

        return new InterestApplicationResponse(account.getId(), account.getAccountType(), previousBalance,
                ratePercentage, interestAmount, newBalance, account.getCurrency());
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }
}
