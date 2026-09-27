# Exploratory session: multi-currency payments

**Charter:** Explore ValuBank's multi-currency payments (payment currency EUR/USD/GBP chosen independently of the source account currency) to find incorrect, unsafe or surprising behaviour. In scope: conversion correctness, same-currency (no conversion) behaviour, interaction with the CHECKING (-5000) / SAVINGS (0) limits and fraud rules. Interfaces: Payments API (:8082), Accounts API (:8081), Currency Rate API (:8085), frontend (:5173). Time-box: 10 minutes.
**Date:** 2026-09-27 (11:22 - 11:26 test activity, report written afterwards)
**Duration:** approx. 4 minutes of active testing plus report writing, within the 10-minute box

Oracle: README.md "Seeded data", "Currency conversion" and "Payment flow" sections, plus the charter.

**Important context:** The environment was **not** in its seeded state at the start. Earlier sessions had already created payments (ids 1-20). Starting balances were: acct 1 = 2500.00 EUR (clean), acct 2 SAVINGS = **-220.00 EUR**, acct 3 CHECKING = **-5094.00 EUR**, acct 4 = -2994.31 USD. All my before/after measurements use balances read immediately before and after each of my payments, so they are not affected by the earlier state.

Published rate table (`GET :8085/api/currency-rates`): GBP->EUR 1.16, GBP->USD 1.27, EUR->USD 1.08, EUR->GBP 0.86, USD->EUR 0.93. There is **no USD->GBP** pair.

## Coverage

- Cross-currency payments from the EUR account (USD and GBP) and from the USD account (EUR and GBP), plus same-currency controls for both.
- The CHECKING -5000 limit when a conversion applies.
- Which amount the pre-debit balance check uses (raw vs converted).
- Validation of currency and amount: unsupported currency (JPY), lowercase currency, zero and negative amounts.
- Fraud: blocked IBAN with a cross-currency payment, and a fraud-limit probe (inconclusive).
- UI: account detail page, currency dropdown, and how a failed GBP payment is shown and listed in the history.

## Findings

### F1: Conversion applies the wrong rate direction (debits the wrong amount)
Observation (account 1, EUR CHECKING):
| Payment | Balance before -> after | Actual debit | Expected debit (README rates) |
|---|---|---|---|
| 100 USD (id 21) | 2500.00 -> 2392.00 | 108.00 EUR | 93.00 EUR (USD->EUR 0.93) |
| 100 GBP (id 22) | 2392.00 -> 2306.00 | 86.00 EUR | 116.00 EUR (GBP->EUR 1.16) |
| 100 EUR (id 23, control) | 2306.00 -> 2206.00 | 100.00 EUR | 100.00 EUR |

Observation (account 4, USD CHECKING): 100 EUR (id 24): -2997.30 -> -3090.30, so the debit was **93.00 USD**. Expected 108.00 USD (EUR->USD 1.08). The 10 USD control (id 26) debited exactly 10.00.

Evidence and conclusion: every actual debit equals amount × rate(account currency -> payment currency), not rate(payment currency -> account currency). Same-currency payments are correct. Conclusion: cross-currency debits are consistently wrong. Customers are overcharged in some directions (USD from EUR: +16%) and undercharged in others (GBP from EUR: -26%). The mechanism (reversed lookup) is a hypothesis that fits all 3 data points.

### F2: A cross-currency payment can push a CHECKING account past the -5000 limit
Observation: account 1 at 2206.00 EUR. Payment of 7000 USD (id 27) returned **COMPLETED**, and the balance became **-5354.00 EUR**, 354 below the documented -5000 floor. A follow-up 1 EUR control payment was correctly REJECTED ("Insufficient funds").
Conclusion: the overdraft rule is breached through a completed payment. The correct conversion (6510 EUR) would have left -4304, which is within limits, so the breach comes from the check and the debit using different amounts (see F3).

### F3: The pre-debit balance check appears to use the raw payment amount, ignoring currency
Observation (account 4, headroom to -5000 = 1982.70 USD):
- 2100 EUR (id 35): REJECTED "Insufficient funds". The reversed-rate debit would have been 1953 (fits), so the check is not using the reversed rate.
- 1900 EUR (id 38): **COMPLETED**. The balance went from -3017.30 to -4784.30 (debit 1767.00 USD, the reversed rate again). A correctly converted amount (2052 USD) exceeds the headroom and should have been rejected.
Hypothesis: the Payments balance check compares the raw payment amount against the account balance with no currency conversion.
Evidence: both results, plus F2 (7000 < 7206 headroom passed), are consistent with this hypothesis. They are inconsistent with a check that uses either the correct or the reversed conversion.
Conclusion: the limit check and the actual debit use different amounts. Even if F1 were fixed, a raw-amount check would still let payments through that breach the limit (e.g. EUR payments from a USD account).

### F4: The UI offers GBP payments from the USD account, but they always FAIL with a misleading reason
Observation: 100 GBP (API, id 25) and 5 GBP (UI, "UI-gbp") from account 4 both return **FAILED - "Accounts service unavailable"**. The Accounts service was up (other calls in the same second succeeded). The balance was unchanged. The UI dropdown shows EUR/USD/GBP for this account.
Evidence: the rate table has no USD->GBP pair, and F1 suggests lookups go account-currency -> payment-currency.
Conclusion: GBP payments from USD accounts are unusable, and the error message wrongly blames service availability. The root cause (missing pair combined with reversed lookup) is a hypothesis.

### F5: Negative amounts are accepted and credit the payer (including across currencies)
Observation: -100 EUR from account 4 (id 30) returned COMPLETED, and the balance went -3100.30 -> **-3007.30** (+93.00 USD). Earlier sessions' history also shows COMPLETED payments of -100 EUR and -5 USD.
Conclusion: a customer can increase their own balance by sending a negative payment. The history shows "-€100.00" as COMPLETED.

### F6: Unsupported or odd currency input is not validated
Observation: JPY 100 (id 32) got past Payments validation and was recorded as FAILED "Accounts service unavailable". Lowercase "usd" (id 33) was COMPLETED, stored as "usd", and debited 10.00. The earlier history contains a COMPLETED payment displayed as "10 null" (created in a prior session with no currency). Amount 0 (id 31) was COMPLETED.
Conclusion: the payment currency is not restricted to EUR/USD/GBP at the API. Invalid values end up either as misleading FAILED records or as COMPLETED records with non-canonical or null currency.

## Notes and questions

- Pre-existing state: SAVINGS account 2 was at -220.00 EUR and CHECKING account 3 at -5094.00 EUR before my session. Account 2's history shows "savings step1" 1500 EUR and "savings probe" 9000 USD, both COMPLETED. This suggests the SAVINGS >= 0 rule was breached through a cross-currency payment in an earlier session, likely via the same raw-amount-check mechanism as F3. I did not reproduce this myself (no SAVINGS headroom left, and I could not reset data).
- Recipients are not credited: account 3 stayed at -5094.00 despite receiving several COMPLETED payments. The README does not say whether internal transfers should credit the destination, so I cannot judge this.
- Fraud limit and currency: is the 10,000 limit meant to be in the payment currency or a normalised currency? 9500 GBP (about 11,020 EUR) was REJECTED for "Insufficient funds" before reaching fraud, so this is untested. Earlier history shows 10001 in each currency being rejected by fraud.
- Payment history shows only the payment-currency amount, never the converted debit, so customers cannot see what was actually taken from their account. The rules do not say whether this is expected.
- Interest on a negative balance: I accidentally called `PUT /api/accounts/4/interest`. The balance went from -2994.31 to -2997.30, so "interest" was charged on the overdraft. This is outside the charter, and the rules do not say whether it is intended.
- The rate table lacks USD->GBP. The README explicitly says not every pair is guaranteed, so its absence alone is not a defect. How the system handles it (F4) is the concern.

## Not covered

- Reproducing a SAVINGS < 0 breach myself (no clean SAVINGS account available, no reset allowed).
- Fraud limit with converted amounts (blocked by insufficient funds on every account with meaningful volume).
- Rounding and precision in conversion (e.g. 0.01 amounts, many decimal places).
- Behaviour when the Currency Rate Service is down (not allowed to stop services).
- UI-side validation of negative or zero amounts (tested only via the API).
- Alice's accounts through the UI, and the admin view.
