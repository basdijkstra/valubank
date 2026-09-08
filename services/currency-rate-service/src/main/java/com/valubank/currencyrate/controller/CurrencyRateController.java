package com.valubank.currencyrate.controller;

import com.valubank.currencyrate.dto.CurrencyRateDto;
import com.valubank.currencyrate.service.CurrencyRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Currency exchange rate API, consumed server-to-server by the
 * ValuBank Accounts Service.
 */
@RestController
@RequestMapping("/api/currency-rates")
public class CurrencyRateController {

    private final CurrencyRateService currencyRateService;

    public CurrencyRateController(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    @GetMapping
    public ResponseEntity<List<CurrencyRateDto>> getAllRates() {
        return ResponseEntity.ok(currencyRateService.getAllRates());
    }

    @GetMapping("/{from}/{to}")
    public ResponseEntity<CurrencyRateDto> getRate(@PathVariable String from, @PathVariable String to) {
        return ResponseEntity.ok(currencyRateService.getRate(from, to));
    }
}
