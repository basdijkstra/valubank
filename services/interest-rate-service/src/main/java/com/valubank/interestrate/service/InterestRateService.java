package com.valubank.interestrate.service;

import com.valubank.interestrate.dto.InterestRateDto;
import com.valubank.interestrate.entity.InterestRateConfig;
import com.valubank.interestrate.exception.InterestRateNotFoundException;
import com.valubank.interestrate.repository.InterestRateConfigRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InterestRateService {

    private final InterestRateConfigRepository repository;

    public InterestRateService(InterestRateConfigRepository repository) {
        this.repository = repository;
    }

    public List<InterestRateDto> getAllRates() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public InterestRateDto getRate(String accountType) {
        InterestRateConfig config = repository.findByAccountTypeIgnoreCase(accountType)
                .orElseThrow(() -> new InterestRateNotFoundException(accountType));
        return toDto(config);
    }

    public InterestRateDto upsertRate(String accountType, BigDecimal ratePercentage) {
        InterestRateConfig config = repository.findByAccountTypeIgnoreCase(accountType)
                .orElseGet(() -> new InterestRateConfig(accountType, ratePercentage));

        config.setRatePercentage(ratePercentage);
        if (config.getAccountType() == null) {
            config.setAccountType(accountType);
        }

        InterestRateConfig saved = repository.save(config);
        return toDto(saved);
    }

    private InterestRateDto toDto(InterestRateConfig config) {
        return new InterestRateDto(config.getAccountType(), config.getRatePercentage());
    }
}
