package com.valubank.accounts.service;

import com.valubank.accounts.client.CurrencyRateClient;
import com.valubank.accounts.client.InterestRateClient;
import com.valubank.accounts.dto.AccountDto;
import com.valubank.accounts.dto.AdminAccountDto;
import com.valubank.accounts.dto.BalanceMutationRequest;
import com.valubank.accounts.dto.CurrencyRateServiceRate;
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
    private final CurrencyRateClient currencyRateClient;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository,
                           InterestRateClient interestRateClient, CurrencyRateClient currencyRateClient) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.interestRateClient = interestRateClient;
        this.currencyRateClient = currencyRateClient;
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

    // CHECKING accounts may run an overdraft down to this floor; SAVINGS accounts may not go negative at all.
    private static final BigDecimal CHECKING_OVERDRAFT_FLOOR = BigDecimal.valueOf(-5000);

    public AccountDto applyBalanceMutation(Long accountId, BalanceMutationRequest request) {
        Account account = findAccountOrThrow(accountId);
        BigDecimal amount = request.getAmount();

        BigDecimal newBalance;
        if ("DEBIT".equalsIgnoreCase(request.getType())) {
            BigDecimal floor = "CHECKING".equalsIgnoreCase(account.getAccountType())
                    ? CHECKING_OVERDRAFT_FLOOR
                    : BigDecimal.ZERO;
            if (account.getBalance().subtract(amount).compareTo(floor) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }
            BigDecimal convertedAmount = convertToAccountCurrency(amount, request.getCurrency(), account.getCurrency());
            newBalance = account.getBalance().subtract(convertedAmount);
        } else {
            BigDecimal convertedAmount = convertToAccountCurrency(amount, request.getCurrency(), account.getCurrency());
            newBalance = account.getBalance().add(convertedAmount);
        }

        account.setBalance(newBalance);
        accountRepository.save(account);
        return AccountDto.from(account);
    }

    // Converts a mutation amount into the account's own currency, if needed. A null
    // mutation currency (or one that already matches the account) is treated as
    // "already in the right currency" - no exchange rate lookup, no dependency call.
    private BigDecimal convertToAccountCurrency(BigDecimal amount, String mutationCurrency, String accountCurrency) {
        if (mutationCurrency == null || mutationCurrency.equalsIgnoreCase(accountCurrency)) {
            return amount;
        }

        CurrencyRateServiceRate rate = currencyRateClient.getRate(accountCurrency, mutationCurrency);
        return amount.multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP);
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
