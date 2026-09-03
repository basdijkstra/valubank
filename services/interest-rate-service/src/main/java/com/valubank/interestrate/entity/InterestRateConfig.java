package com.valubank.interestrate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * A single interest rate configuration entry for a given account type,
 * e.g. accountType="CHECKING", ratePercentage=0.1
 */
@Entity
@Table(name = "interest_rate_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = "account_type")
})
public class InterestRateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "rate_percentage", nullable = false)
    private BigDecimal ratePercentage;

    protected InterestRateConfig() {
        // required by JPA
    }

    public InterestRateConfig(String accountType, BigDecimal ratePercentage) {
        this.accountType = accountType;
        this.ratePercentage = ratePercentage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }
}
