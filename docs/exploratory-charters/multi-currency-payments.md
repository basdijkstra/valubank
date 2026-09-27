# Exploratory charter: multi-currency payments

**Mission:** Explore ValuBank's new multi-currency payment functionality to discover incorrect, unsafe, or surprising behaviour.

Focus particularly on whether payments behave correctly when the payment currency is the same as, or different from, the source account's currency.

The goal is to learn about the behaviour of the feature and provide valuable feedback, not merely to confirm the acceptance criteria.

**Product context:**
ValuBank customers can now select a payment currency independently of the currency of their source account.

The supported payment currencies are:

* EUR
* USD
* GBP

When the payment currency differs from the account currency, the payment amount is converted to the account currency before the account is debited. When the payment currency matches the account currency, no currency conversion takes place.

Existing account and payment rules continue to apply, including:

* CHECKING accounts may go down to -5000.
* SAVINGS accounts may not go below 0.
* Existing fraud detection rules apply to payments.

**Exploration:**
Explore the feature using the available ValuBank testing interfaces.

Consider different account and payment currencies, payment amounts, and account situations.

Follow interesting observations and investigate behaviour that appears incorrect, unexpected, or surprising.

Use your available testing time to decide what is worth investigating.

**Time-box:** 10 minutes.

**Test data notes:** Bob has a second CHECKING account in USD
(`NL01VALU0000000004`) alongside his EUR account - useful for cross-currency
scenarios. See README.md's "Seeded data" section for full account/currency
list and business rules.
