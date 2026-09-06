package com.valubank.accounts.service;

import com.valubank.accounts.client.InterestRateClient;
import com.valubank.accounts.dto.InterestApplicationResponse;
import com.valubank.accounts.dto.InterestRateServiceRate;
import com.valubank.accounts.entity.Account;
import com.valubank.accounts.repository.AccountRepository;
import com.valubank.accounts.repository.CustomerRepository;
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

    @Test
    void applyInterest_appliesEachTierRateToItsPortionOfTheBalance() {
        
        // Arrange - real domain objects, no mocking needed for plain in-process data.
        Account account = new Account(1L, "NL01VALU0000000002", "SAVINGS",
                new BigDecimal("12000.00"), "EUR");
        account.setId(2L);

        List<InterestRateServiceRate.Tier> tiers = List.of(
                new InterestRateServiceRate.Tier(new BigDecimal("10000"), new BigDecimal("1.5")),
                new InterestRateServiceRate.Tier(null, new BigDecimal("1.0"))
        );

        when(accountRepository.findById(2L)).thenReturn(java.util.Optional.of(account));
        when(interestRateClient.getRateForAccountType("SAVINGS"))
                .thenReturn(new InterestRateServiceRate("SAVINGS", tiers));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InterestApplicationResponse response = accountService.applyInterest(2L);

        // Assert - 1.5% on 10,000 (150.00) + 1.0% on the remaining 2,000 (20.00) = 170.00.
        assertThat(response.getPreviousBalance()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(response.getInterestAmount()).isEqualTo(new BigDecimal("170.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("12170.00"));
    }
}
