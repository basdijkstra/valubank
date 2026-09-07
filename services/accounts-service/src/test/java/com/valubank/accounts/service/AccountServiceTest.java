package com.valubank.accounts.service;

import com.valubank.accounts.client.InterestRateClient;
import com.valubank.accounts.dto.InterestApplicationResponse;
import com.valubank.accounts.dto.InterestRateServiceRate;
import com.valubank.accounts.entity.Account;
import com.valubank.accounts.repository.AccountRepository;
import com.valubank.accounts.repository.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    // Cross process/IO boundaries - mocked so the test is fast and deterministic,
    // with no real HTTP call or database involved.
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InterestRateClient interestRateClient;

    // Class under test - real instance, wired with the mocks above.
    @InjectMocks
    private AccountService accountService;

    private List<InterestRateServiceRate.Tier> tiers;

    @BeforeEach 
    public void setupInterestRateTiers() {

        // For savings account, the first 10,000 earns 1.5% and any remaining balance earns 1.0%.
        this.tiers = List.of(
                new InterestRateServiceRate.Tier(new BigDecimal("10000"), new BigDecimal("1.5")),
                new InterestRateServiceRate.Tier(null, new BigDecimal("1.0"))
        );
    }

    @Test
    void applyInterest_appliesEachTierRateToItsPortionOfTheBalance() {
        
        // Arrange

        /**
         * TODO: Create a new Account instance with the following properties:
         * - customerId: 1L
         * - iban: "NL01VALU0000000002"
         * - accountType: "SAVINGS"
         * - balance: 12000.00
         * - currency: "EUR"
         * 
         * Then, set the account ID to 2L.
         */
        Account account = null;

        when(accountRepository.findById(2L)).thenReturn(java.util.Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        /**
         * TODO: Mock the interestRateClient.getRateForAccountType("SAVINGS") method
         * to return an InterestRateServiceRate with accountType "SAVINGS" and the tiers
         * defined in the setupInterestRateTiers() @BeforeEach method.
         */
        

        // Act

        /**
         * TODO: Call the applyInterest method of the accountService with accountId 2L
         */
        InterestApplicationResponse response = null;

        // Assert

        /**
         * TODO: Assert that the response has the following properties:
         * - previousBalance: 12000.00
         * - interestAmount: 170.00
         * - new balance of the account: 12170.00
         * 
         * Also check that the account's balance has indeed been updated to 12170.00.
         */
    
    }
}
