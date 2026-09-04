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
        BigDecimal balance = account.getBalance();

        InterestRateServiceRate rate = interestRateClient.getRateForAccountType(account.getAccountType());
        BigDecimal interestAmount = interestAmountForTiers(rate.getTiers(), balance);
        BigDecimal ratePercentage = effectiveRatePercentage(balance, interestAmount);

        return new InterestRateResponse(account.getId(), account.getAccountType(), ratePercentage.doubleValue());
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

    // Calculates interest on the current balance, using the account type's tier schedule
    // from the Interest Rate / Configuration Service, and credits it to the account.
    public InterestApplicationResponse applyInterest(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        BigDecimal previousBalance = account.getBalance();

        InterestRateServiceRate rate = interestRateClient.getRateForAccountType(account.getAccountType());
        BigDecimal interestAmount = interestAmountForTiers(rate.getTiers(), previousBalance);

        BigDecimal newBalance = previousBalance.add(interestAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Effective blended rate, derived from the interest actually applied, so the
        // response stays meaningful even when the balance spans multiple tiers.
        BigDecimal ratePercentage = effectiveRatePercentage(previousBalance, interestAmount);

        return new InterestApplicationResponse(account.getId(), account.getAccountType(), previousBalance,
                ratePercentage, interestAmount, newBalance, account.getCurrency());
    }

    // Applies each tier's rate only to the portion of the balance that falls within it -
    // e.g. tiers [{upTo:10000, rate:1.5}, {upTo:null, rate:1.0}] on a balance of 12000
    // earns 1.5% on 10000 and 1.0% on the remaining 2000. Works for any number of tiers,
    // so it needs no per-account-type branching.
    private BigDecimal interestAmountForTiers(List<InterestRateServiceRate.Tier> tiers, BigDecimal balance) {
        BigDecimal previousThreshold = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (InterestRateServiceRate.Tier tier : tiers) {
            BigDecimal tierCap = tier.getUpToAmount() == null ? balance : tier.getUpToAmount();
            BigDecimal tierBalance = balance.min(tierCap).subtract(previousThreshold).max(BigDecimal.ZERO);

            totalInterest = totalInterest.add(tierBalance
                    .multiply(tier.getRatePercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

            previousThreshold = tierCap;
            if (balance.compareTo(previousThreshold) <= 0) {
                break;
            }
        }

        return totalInterest;
    }

    private BigDecimal effectiveRatePercentage(BigDecimal balance, BigDecimal interestAmount) {
        return balance.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : interestAmount.multiply(BigDecimal.valueOf(100))
                        .divide(balance, 4, RoundingMode.HALF_UP);
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }
}
