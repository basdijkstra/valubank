package com.valubank.currencyrate.controller;

import com.valubank.currencyrate.dto.CurrencyRateDto;
import com.valubank.currencyrate.dto.ErrorResponse;
import com.valubank.currencyrate.exception.CurrencyRateNotFoundException;
import com.valubank.currencyrate.service.CurrencyRateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CurrencyRateController {

    private final CurrencyRateService currencyRateService;

    public CurrencyRateController(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    @GetMapping("/api/currency-rates/{from}/{to}")
    public CurrencyRateDto getRate(@PathVariable String from, @PathVariable String to) {
        return currencyRateService.getRate(from, to);
    }

    @GetMapping("/api/currency-rates")
    public List<CurrencyRateDto> getAllRates() {
        return currencyRateService.getAllRates();
    }

    @ExceptionHandler(CurrencyRateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CurrencyRateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }
}
