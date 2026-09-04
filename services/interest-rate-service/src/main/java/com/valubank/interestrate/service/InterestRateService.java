package com.valubank.interestrate.service;

import com.valubank.interestrate.dto.InterestRateDto;
import com.valubank.interestrate.dto.InterestRateTierDto;
import com.valubank.interestrate.entity.InterestRateTier;
import com.valubank.interestrate.exception.InterestRateNotFoundException;
import com.valubank.interestrate.repository.InterestRateTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InterestRateService {

    private final InterestRateTierRepository repository;

    public InterestRateService(InterestRateTierRepository repository) {
        this.repository = repository;
    }

    public List<InterestRateDto> getAllRates() {
        Map<String, List<InterestRateTier>> byAccountType = repository.findAllByOrderByAccountTypeAscTierOrderAsc()
                .stream()
                .collect(Collectors.groupingBy(InterestRateTier::getAccountType, LinkedHashMap::new, Collectors.toList()));

        return byAccountType.entrySet().stream()
                .map(entry -> toDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    public InterestRateDto getRate(String accountType) {
        List<InterestRateTier> tiers = repository.findByAccountTypeIgnoreCaseOrderByTierOrderAsc(accountType);
        if (tiers.isEmpty()) {
            throw new InterestRateNotFoundException(accountType);
        }
        return toDto(accountType, tiers);
    }

    // Replaces the full tier schedule for an account type in one go, rather than
    // patching individual tiers, so the schedule can never end up partially updated.
    @Transactional
    public InterestRateDto upsertRates(String accountType, List<InterestRateTierDto> tierRequests) {
        if (tierRequests == null || tierRequests.isEmpty()) {
            throw new IllegalArgumentException("At least one tier is required");
        }

        repository.deleteByAccountTypeIgnoreCase(accountType);

        List<InterestRateTier> saved = new ArrayList<>();
        int order = 0;
        for (InterestRateTierDto tierRequest : tierRequests) {
            InterestRateTier tier = new InterestRateTier(accountType, order++,
                    tierRequest.getUpToAmount(), tierRequest.getRatePercentage());
            saved.add(repository.save(tier));
        }

        return toDto(accountType, saved);
    }

    private InterestRateDto toDto(String accountType, List<InterestRateTier> tiers) {
        List<InterestRateTierDto> tierDtos = tiers.stream()
                .map(tier -> new InterestRateTierDto(tier.getUpToAmount(), tier.getRatePercentage()))
                .toList();
        return new InterestRateDto(accountType, tierDtos);
    }
}
