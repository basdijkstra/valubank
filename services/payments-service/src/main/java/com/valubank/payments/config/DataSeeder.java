package com.valubank.payments.config;

import com.valubank.payments.entity.Payment;
import com.valubank.payments.repository.PaymentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds payment history on startup. Written in plain Java (instead of data.sql) so workshop
 * participants can easily read and tweak it.
 *
 * These payments are inserted directly into this service's own database - they do not go
 * through the real payment flow, so they never call the Accounts Service. Account balances
 * seeded by the Accounts Service are left exactly as documented in the README.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final String COMPLETED = "COMPLETED";
    private static final String REJECTED = "REJECTED";
    private static final String FRAUD_REASON = "Destination account is flagged for fraud";

    // Account IDs, matching the Accounts Service's seed order (see its DataSeeder):
    // 1 = alice CHECKING (EUR), 2 = alice SAVINGS (EUR), 3 = bob CHECKING (EUR), 4 = bob CHECKING (USD)
    private static final Long ALICE_CHECKING = 1L;
    private static final Long ALICE_SAVINGS = 2L;
    private static final Long BOB_CHECKING_EUR = 3L;
    private static final Long BOB_CHECKING_USD = 4L;

    private final PaymentRepository paymentRepository;

    public DataSeeder(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAliceChecking();
        seedAliceSavings();
        seedBobCheckingEur();
        seedBobCheckingUsd();
    }

    private void seedAliceChecking() {
        Long id = ALICE_CHECKING;
        save(id, "NL77RABO0123456789", "Vastgoed Beheer BV", "1200.00", "EUR", "Monthly rent", 40, COMPLETED, null);
        save(id, "NL22INGB0001234567", "Albert Heijn", "84.75", "EUR", "Groceries", 35, COMPLETED, null);
        save(id, "NL33ABNA0009876543", "Energie Direct", "145.20", "EUR", "Utility bill", 30, COMPLETED, null);
        save(id, "NL44RABO0011223344", "Netflix", "13.99", "EUR", "Subscription", 27, COMPLETED, null);
        save(id, "NL55INGB0022334455", "De Gouden Lepel", "62.50", "EUR", "Restaurant", 24, COMPLETED, null);
        save(id, "NL66ABNA0033445566", "FitClub Gym", "39.00", "EUR", "Gym membership", 20, COMPLETED, null);
        save(id, "NL77RABO0099998888", "CloudSoft Inc", "29.00", "USD", "Software subscription", 16, COMPLETED, null);
        save(id, "NL01VALU0000000003", "Bob de Vries", "50.00", "EUR", "Splitting dinner", 12, COMPLETED, null);
        save(id, "NL88ABNA0044556677", "Zorgverzekeraar", "132.45", "EUR", "Health insurance", 8, COMPLETED, null);
        save(id, "NL99BLOCKED0000000", "Unknown Retailer", "250.00", "EUR", "Online purchase", 3, REJECTED, FRAUD_REASON);
    }

    private void seedAliceSavings() {
        Long id = ALICE_SAVINGS;
        save(id, "NL01VALU0000000001", "Alice Janssen", "500.00", "EUR", "Transfer to checking", 50, COMPLETED, null);
        save(id, "NL01VALU0000000001", "Alice Janssen", "300.00", "EUR", "Transfer to checking", 38, COMPLETED, null);
        save(id, "NL10RABO0055667788", "Reisbureau Zon", "1450.00", "EUR", "Holiday booking", 33, COMPLETED, null);
        save(id, "NL01VALU0000000001", "Alice Janssen", "200.00", "EUR", "Transfer to checking", 25, COMPLETED, null);
        save(id, "NL20INGB0066778899", "MediaMarkt", "899.00", "EUR", "New laptop", 21, COMPLETED, null);
        save(id, "NL30ABNA0077889900", "Notaris Kantoor", "650.00", "EUR", "Legal fees", 17, COMPLETED, null);
        save(id, "NL01VALU0000000001", "Alice Janssen", "400.00", "EUR", "Transfer to checking", 13, COMPLETED, null);
        save(id, "NL40RABO0088990011", "London Antiques Ltd", "320.00", "GBP", "Antique purchase", 9, COMPLETED, null);
        save(id, "NL50INGB0099001122", "Zonnepanelen Direct", "2200.00", "EUR", "Solar panels down payment", 5, COMPLETED, null);
        save(id, "NL01VALU0000000001", "Alice Janssen", "150.00", "EUR", "Transfer to checking", 2, COMPLETED, null);
    }

    private void seedBobCheckingEur() {
        Long id = BOB_CHECKING_EUR;
        save(id, "NL60RABO0011002200", "Woningstichting Utrecht", "725.00", "EUR", "Monthly rent", 42, COMPLETED, null);
        save(id, "NL22INGB0001234567", "Albert Heijn", "46.10", "EUR", "Groceries", 36, COMPLETED, null);
        save(id, "NL70ABNA0022003300", "Vodafone", "45.00", "EUR", "Phone bill", 31, COMPLETED, null);
        save(id, "NL80RABO0033004400", "Spotify", "10.99", "EUR", "Subscription", 27, COMPLETED, null);
        save(id, "NL01VALU0000000001", "Alice Janssen", "50.00", "EUR", "Splitting dinner", 22, COMPLETED, null);
        save(id, "NL90INGB0044005500", "Praxis", "89.99", "EUR", "DIY supplies", 18, COMPLETED, null);
        save(id, "NL11ABNA0055006600", "Fietsenwinkel Centraal", "180.00", "EUR", "Bicycle repair", 14, COMPLETED, null);
        save(id, "NL21RABO0066007700", "Global Games Store", "25.00", "USD", "Game purchase", 10, COMPLETED, null);
        save(id, "NL22ABNA0077008800", "Ziekenhuis Utrecht", "95.50", "EUR", "Medical costs", 6, COMPLETED, null);
        save(id, "NL99BLOCKED0000000", "Suspicious Vendor", "300.00", "EUR", "Online purchase", 2, REJECTED, FRAUD_REASON);
    }

    private void seedBobCheckingUsd() {
        Long id = BOB_CHECKING_USD;
        save(id, "NL30RABO0088009900", "US Rent Co", "600.00", "USD", "Monthly rent", 39, COMPLETED, null);
        save(id, "NL40INGB0099001100", "Whole Foods", "78.40", "USD", "Groceries", 34, COMPLETED, null);
        save(id, "NL50ABNA0000112233", "AT&T", "55.00", "USD", "Phone bill", 29, COMPLETED, null);
        save(id, "NL60RABO0011223344", "Disney Plus", "11.99", "USD", "Subscription", 25, COMPLETED, null);
        save(id, "NL01VALU0000000003", "Bob de Vries", "100.00", "USD", "Transfer between own accounts", 20, COMPLETED, null);
        save(id, "NL70INGB0022334455", "Best Buy", "249.00", "USD", "Electronics purchase", 16, COMPLETED, null);
        save(id, "NL80ABNA0033445566", "European Bookstore", "40.00", "EUR", "Book purchase", 12, COMPLETED, null);
        save(id, "NL90RABO0044556677", "Gym USA", "35.00", "USD", "Gym membership", 8, COMPLETED, null);
        save(id, "NL00INGB0055667788", "City Hospital", "120.00", "USD", "Medical costs", 4, COMPLETED, null);
        save(id, "NL99BLOCKED0000000", "Fraud Vendor", "500.00", "USD", "Online purchase", 1, REJECTED, FRAUD_REASON);
    }

    private void save(Long fromAccountId, String toIban, String toName, String amount, String currency,
                       String description, int daysAgo, String status, String reason) {
        Payment payment = new Payment();
        payment.setFromAccountId(fromAccountId);
        payment.setToAccountIban(toIban);
        payment.setToAccountName(toName);
        payment.setAmount(new BigDecimal(amount));
        payment.setCurrency(currency);
        payment.setDescription(description);
        payment.setStatus(status);
        payment.setReason(reason);
        payment.setTimestamp(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        paymentRepository.save(payment);
    }
}
