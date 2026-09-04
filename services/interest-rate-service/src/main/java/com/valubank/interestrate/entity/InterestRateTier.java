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
 * One balance tier of the interest rate schedule for an account type, e.g.
 * accountType="SAVINGS", tierOrder=0, upToAmount=10000.00, ratePercentage=1.5
 * means: the portion of the balance up to and including 10,000 earns 1.5%.
 * upToAmount=null marks the last (unbounded) tier, covering everything above
 * the previous tier's threshold.
 */
@Entity
@Table(name = "interest_rate_tier", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_type", "tier_order"})
})
public class InterestRateTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "tier_order", nullable = false)
    private int tierOrder;

    @Column(name = "up_to_amount")
    private BigDecimal upToAmount;

    @Column(name = "rate_percentage", nullable = false)
    private BigDecimal ratePercentage;

    protected InterestRateTier() {
        // required by JPA
    }

    public InterestRateTier(String accountType, int tierOrder, BigDecimal upToAmount, BigDecimal ratePercentage) {
        this.accountType = accountType;
        this.tierOrder = tierOrder;
        this.upToAmount = upToAmount;
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

    public int getTierOrder() {
        return tierOrder;
    }

    public void setTierOrder(int tierOrder) {
        this.tierOrder = tierOrder;
    }

    public BigDecimal getUpToAmount() {
        return upToAmount;
    }

    public void setUpToAmount(BigDecimal upToAmount) {
        this.upToAmount = upToAmount;
    }

    public BigDecimal getRatePercentage() {
        return ratePercentage;
    }

    public void setRatePercentage(BigDecimal ratePercentage) {
        this.ratePercentage = ratePercentage;
    }
}
