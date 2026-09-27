# Exploratory charter: multi-currency payments

**Mission:** Explore payments made in a currency other than the source
account's own currency, to find places where actual behavior diverges from
the documented conversion, overdraft, and fraud rules.

**Scope / focus areas:**
- Making a payment where the selected currency differs from the source
  account's currency (EUR, USD, GBP).
- Interaction between currency conversion and the overdraft floor
  (CHECKING accounts down to -5000, SAVINGS accounts not below 0).
- Interaction between currency conversion and fraud rejection (payments
  over 10,000, blocked IBAN).
- Whether the amount actually debited (check via the account/API, not just
  the success message) matches what the documented exchange behavior would
  predict.
- Payments in a currency not currently used by any seeded account.

**Out of scope:** Login/session handling, admin dashboard, interest
calculation, IBAN format validation.

**Risk areas / hints:** Cross-currency conversion, and its ordering
relative to other checks (overdraft, fraud), are new. Pay attention to
whether the *order* checks happen in produces a different result than you'd
expect from reading the documented rules independently. Also check what
happens for a currency pair or account currency the app doesn't obviously
support.

**Time-box:** 10 minutes.

**Test data notes:** Bob has a second CHECKING account in USD
(`NL01VALU0000000004`) alongside his EUR account - useful for cross-currency
scenarios. See README.md's "Seeded data" section for full account/currency
list and business rules.
