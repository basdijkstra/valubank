### Support payments in a currency other than the account's own

#### Description
  Currently, payments can only be made in the source account's own currency. This story adds support for
  sending a payment in a different currency (EUR, USD, or GBP), with the amount automatically converted to the
  source account's currency before the balance is debited. This brings ValuBank closer to how real
  multi-currency banking platforms handle cross-currency transfers.

#### Acceptance Criteria:

  1. When making a payment, the user can select a payment currency (EUR, USD, or GBP) independent of the
     source account's own currency.
  2. If the selected payment currency differs from the account's currency, the amount is converted to the
     account's currency before the balance is debited.
  3. If the selected payment currency matches the account's currency, no conversion takes place — the amount is debited as entered.
  4. Existing overdraft rules (CHECKING accounts may go negative down to -5000; SAVINGS accounts may not go
     negative) continue to be enforced correctly for converted payments.
  5. Existing fraud detection rules continue to apply to payments made in this feature.