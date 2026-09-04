package com.valubank.interestrate.controller;

import com.valubank.interestrate.dto.InterestRateDto;
import com.valubank.interestrate.dto.UpdateRateRequest;
import com.valubank.interestrate.service.InterestRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Interest rate configuration API, consumed server-to-server by the
 * ValuBank Accounts Service. Each account type has an ordered schedule of
 * balance tiers, each with its own rate.
 */
@RestController
@RequestMapping("/api/interest-rates")
public class InterestRateController {

    private final InterestRateService interestRateService;

    public InterestRateController(InterestRateService interestRateService) {
        this.interestRateService = interestRateService;
    }

    @GetMapping
    public ResponseEntity<List<InterestRateDto>> getAllRates() {
        return ResponseEntity.ok(interestRateService.getAllRates());
    }

    @GetMapping("/{accountType}")
    public ResponseEntity<InterestRateDto> getRate(@PathVariable String accountType) {
        return ResponseEntity.ok(interestRateService.getRate(accountType));
    }

    @PutMapping("/{accountType}")
    public ResponseEntity<InterestRateDto> upsertRate(@PathVariable String accountType,
                                                        @RequestBody UpdateRateRequest request) {
        InterestRateDto updated = interestRateService.upsertRates(accountType, request.getTiers());
        return ResponseEntity.ok(updated);
    }
}
