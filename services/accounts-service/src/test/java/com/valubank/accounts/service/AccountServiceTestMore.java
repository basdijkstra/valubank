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
class AccountServiceTestMore {

    // Cross process/IO boundaries - mocked so the test is fast and deterministic,
    // with no real HTTP call or database involved.
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InterestRateClient interestRateClient;

    @Mock
    private TieredInterestCalculator tieredInterestCalculator;

    // Class under test - real instance, wired with the mocks above.
    @InjectMocks
    private AccountService accountService;

    private List<InterestRateServiceRate.Tier> twoTiers;
    private List<InterestRateServiceRate.Tier> threeTiers;
    private List<InterestRateServiceRate.Tier> emptyTiers;

    @BeforeEach
    public void setupInterestRateTiers() {

        this.twoTiers = List.of(
                new InterestRateServiceRate.Tier(new BigDecimal("10000"), new BigDecimal("1.5")),
                new InterestRateServiceRate.Tier(null, new BigDecimal("1.0"))
        );

        this.threeTiers = List.of(
                new InterestRateServiceRate.Tier(new BigDecimal("10000"), new BigDecimal("1.5")),
                new InterestRateServiceRate.Tier(new BigDecimal("20000"), new BigDecimal("1.0")),
                new InterestRateServiceRate.Tier(null, new BigDecimal("0.5"))
        );

        this.emptyTiers = List.of();
    }

    @Test
    void applyInterest_usingTwoTiers_appliesEachTierRateToItsPortionOfTheBalance() {

        // Arrange
        Account account = new Account(1L, "NL01VALU0000000002", "SAVINGS", new BigDecimal("12000.00"), "EUR");
        account.setId(2L);

        when(accountRepository.findById(2L)).thenReturn(java.util.Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(interestRateClient.getRateForAccountType("SAVINGS"))
                .thenReturn(new InterestRateServiceRate("SAVINGS", twoTiers));

        // Added
        when(tieredInterestCalculator.calculateInterest(twoTiers, new BigDecimal("12000.00")))
                .thenReturn(new BigDecimal("170.00"));

        // Act
        InterestApplicationResponse response = accountService.applyInterest(2L);

        // Assert
        assertThat(response.getPreviousBalance()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(response.getInterestAmount()).isEqualTo(new BigDecimal("170.00"));
        assertThat(response.getNewBalance()).isEqualTo(new BigDecimal("12170.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("12170.00"));
    }

    @Test
    void applyInterest_usingThreeTiers_appliesEachTierRateToItsPortionOfTheBalance() {

        // Arrange
        Account account = new Account(1L, "NL01VALU0000000002", "SAVINGS", new BigDecimal("35000.00"), "EUR");
        account.setId(2L);

        when(accountRepository.findById(2L)).thenReturn(java.util.Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(interestRateClient.getRateForAccountType("SAVINGS"))
                .thenReturn(new InterestRateServiceRate("SAVINGS", threeTiers));

        // Added
        when(tieredInterestCalculator.calculateInterest(threeTiers, new BigDecimal("35000.00")))
                .thenReturn(new BigDecimal("325.00"));

        // Act
        InterestApplicationResponse response = accountService.applyInterest(2L);

        // Assert
        assertThat(response.getPreviousBalance()).isEqualTo(new BigDecimal("35000.00"));
        assertThat(response.getInterestAmount()).isEqualTo(new BigDecimal("325.00"));
        assertThat(response.getNewBalance()).isEqualTo(new BigDecimal("35325.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("35325.00"));
    }

    @Test
    void applyInterest_usingEmptyTiers_appliesNoInterest() {

        // Arrange
        Account account = new Account(1L, "NL01VALU0000000002", "SAVINGS", new BigDecimal("12000.00"), "EUR");
        account.setId(2L);

        when(accountRepository.findById(2L)).thenReturn(java.util.Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(interestRateClient.getRateForAccountType("SAVINGS"))
                .thenReturn(new InterestRateServiceRate("SAVINGS", emptyTiers));

        // Added
        when(tieredInterestCalculator.calculateInterest(emptyTiers, new BigDecimal("12000.00")))
                .thenReturn(BigDecimal.ZERO);

        // Act
        InterestApplicationResponse response = accountService.applyInterest(2L);

        // Assert
        assertThat(response.getPreviousBalance()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(response.getInterestAmount()).isEqualTo(new BigDecimal("0"));
        assertThat(response.getNewBalance()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("12000.00"));
    }
}
