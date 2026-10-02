# Manual test cases: Scheduled payments

Manual test cases for the scheduled payments feature described in
[`docs/scheduled-payments-spec.md`](../scheduled-payments-spec.md). Each test
case checks the UI only in its last step that uses the UI; earlier UI steps
just lead there. API checks (`curl`) can appear in any step. Table-based test
cases are data-driven: every row is a separate run.

## Contents

1. [Conventions](#conventions)
2. [Test environment and test data](#test-environment-and-test-data)
3. [Helper commands](#helper-commands)
4. [Traceability](#traceability)
5. [A. Scheduling via the UI: main flow](#a-scheduling-via-the-ui-main-flow)
6. [B. Input validation and boundary values (UI)](#b-input-validation-and-boundary-values-ui)
7. [D. Account ownership and access](#d-account-ownership-and-access)
8. [E. Scheduled payments list](#e-scheduled-payments-list)
9. [F. Cancelling](#f-cancelling)
10. [H. Balance and fraud boundaries at execution](#h-balance-and-fraud-boundaries-at-execution)
11. [I. Executed exactly once](#i-executed-exactly-once)

---

## Conventions

- **R1-R10** refer to the business rules in the spec:

  | Rule | Text |
  |------|------|
  | R1 | Execution date must be in the future. |
  | R2 | Payment amount must be greater than zero. |
  | R3 | The source account must belong to the logged-in customer. |
  | R4 | Existing payment rules still apply. |
  | R5 | A scheduled payment can be cancelled before execution. |
  | R6 | A scheduled payment cannot be cancelled after execution. |
  | R7 | A payment is executed once, on its scheduled date. |
  | R8 | The scheduled payment appears in the customer's scheduled-payments list. |
  | R9 | After execution, it appears in payment history. |
  | R10 | A failed scheduled payment gets an appropriate status/reason. |

- **Dates.** `D` is today's date *as the Payments Service sees it* (the server's
  local date). `D+1` is tomorrow, `D-1` is yesterday, and so on. Enter dates in
  the date picker in your browser's format. In API calls, use `YYYY-MM-DD`.
- **Amounts** are entered without thousands separators (`7500.00`, not `7,500.00`).
- **"Clarify"** marks a verification where the spec does not define the
  expected result. Record what you observe and raise it with the product owner;
  do not mark the test as failed for it.
- **"Known risk"** marks a verification where the risk analysis suggests the
  implementation may not meet the spec. If it doesn't, log a defect and
  reference the rule.
- Record a test as **passed** only when *every* verification of *every* step
  holds.

## Test environment and test data

**Prerequisites**

- All services and the frontend are running (`scripts/start-all.ps1` or
  `scripts/start-all.sh`):

  | Component | URL |
  |-----------|-----|
  | Frontend | http://localhost:5173 |
  | Accounts Service | http://localhost:8081 |
  | Payments Service | http://localhost:8082 |
  | Fraud Service | http://localhost:8083 |

- `valubank.test-support.enabled` is `true` in the Payments Service's
  `application.yml` (the default). This enables the "execute now" endpoint
  used in sections E, F, H and I.
- A terminal with `curl` is available, for example Git Bash on Windows. The
  examples use bash quoting.

**Seed data** (reset on every restart of the Accounts Service)

| Customer | Login | Customer id | Account id | IBAN | Type | Balance | Currency |
|----------|-------|-------------|------------|------|------|---------|----------|
| Alice Janssen | `alice` / `password123` | 1 | 1 | NL01VALU0000000001 | CHECKING | 2,500.00 | EUR |
| Alice Janssen | | 1 | 2 | NL01VALU0000000002 | SAVINGS | 11,000.00 | EUR |
| Bob de Vries | `bob` / `password123` | 2 | 3 | NL01VALU0000000003 | CHECKING | 500.00 | EUR |
| Bob de Vries | | 2 | 4 | NL01VALU0000000004 | CHECKING | 750.00 | USD |
| ValuBank Admin | `admin` / `admin123` | 3 | - | - | - | - | - |

Before relying on these ids, confirm them once with the helper commands below.

**Rules that apply at execution time** (the existing payment rules, R4)

| Rule | Owner | Boundary |
|------|-------|----------|
| Maximum amount per transaction | Fraud Service | `10000.00` allowed, `10000.01` rejected |
| Blocked beneficiary | Fraud Service | `NL99BLOCKED0000000` rejected |
| CHECKING overdraft | Accounts Service | balance may go down to `-5000.00` |
| SAVINGS floor | Accounts Service | balance may not go below `0.00` |

**Resetting data**

- Restarting the **Accounts Service** resets balances to the seed values. It does
  not touch scheduled payments or payment history.
- Restarting the **Payments Service** deletes all scheduled payments and payment
  history.
- For a full reset, run `scripts/stop-all.*` followed by `scripts/start-all.*`.

**Stopping and starting a single service** (TC-SP-43)

```powershell
# PowerShell: stop whatever is listening on a port (here: Accounts Service)
Get-NetTCPConnection -LocalPort 8081 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

```bash
# Start it again (from the repository root)
cd services/accounts-service && mvn spring-boot:run
```

## Helper commands

```bash
# Look up an account (balance, owner, currency)
curl -s http://localhost:8081/api/accounts/1

# List a customer's scheduled payments
curl -s http://localhost:8082/api/customers/1/scheduled-payments

# Payment history of an account
curl -s http://localhost:8082/api/accounts/1/payments

# Schedule a payment (-i shows the HTTP status)
curl -s -i -X POST http://localhost:8082/api/scheduled-payments \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"fromAccountId":1,"toAccountIban":"NL01VALU0000000003","toAccountName":"Bob de Vries","amount":10.00,"executionDate":"YYYY-MM-DD","description":"test"}'

# Cancel a scheduled payment
curl -s -i -X POST "http://localhost:8082/api/scheduled-payments/{id}/cancel?customerId=1"

# TEST-ONLY: execute a scheduled payment now, ignoring its execution date
curl -s -i -X POST http://localhost:8082/api/test-support/scheduled-payments/{id}/execute
```

The id of a scheduled payment is in the API responses, or in the UI as the
`data-testid="scheduled-payment-{id}"` attribute of its list item (use the
browser's developer tools).

## Traceability

| Rule | Test cases |
|------|------------|
| R1 | TC-SP-01, TC-SP-13, TC-SP-14 |
| R2 | TC-SP-11, TC-SP-12 |
| R3 | TC-SP-03, TC-SP-30 to TC-SP-37, TC-SP-53 |
| R4 | TC-SP-04, TC-SP-70 to TC-SP-73 |
| R5 | TC-SP-50 to TC-SP-53 |
| R6 | TC-SP-54, TC-SP-55, TC-SP-56, TC-SP-82 |
| R7 | TC-SP-51, TC-SP-73, TC-SP-80 to TC-SP-82 |
| R8 | TC-SP-01, TC-SP-33, TC-SP-40 to TC-SP-43 |
| R9 | TC-SP-70, TC-SP-80 |
| R10 | TC-SP-42, TC-SP-55, TC-SP-70 to TC-SP-73 |

The test cases are numbered per section (A = 0x, B = 1x, D = 3x, E = 4x,
F = 5x, H = 7x, I = 8x), so numbers are not consecutive.

---

## A. Scheduling via the UI: main flow

### TC-SP-01: Schedule a valid payment for tomorrow

**Covers:** R1, R8 · **Preconditions:** Full data reset. Alice has no scheduled payments.

1. Open http://localhost:5173 and log in as `alice` / `password123`.
2. Click **Scheduled payments** in the header.
3. Fill in: Beneficiary IBAN `NL01VALU0000000003`, Beneficiary name `Bob de Vries`, Amount `25.50`, Execution date `D+1`, Description `Concert tickets`.
4. Click **Review payment**.
5. Click **Confirm**.
   - **Verify:** a success banner shows "**SCHEDULED** - payment scheduled for `D+1`".
   - **Verify:** the form is empty again, except that **From account** is still `NL01VALU0000000001`.
   - **Verify:** the right card lists one entry: `Bob de Vries`, status `SCHEDULED`, `NL01VALU0000000003`, `€25.50`, "From NL01VALU0000000001", "Execution date: `D+1`" and the description `Concert tickets`.
6. Run `curl -s http://localhost:8081/api/accounts/1`.
   - **Verify:** the balance is still `2500.00` (scheduling does not debit).
7. Run `curl -s http://localhost:8082/api/accounts/1/payments`.
   - **Verify:** the response is `[]` (scheduling creates no payment in history).
8. Run `curl -s http://localhost:8082/api/customers/1/scheduled-payments`.
   - **Verify:** one entry with `status` `SCHEDULED`, `amount` `25.5`, `currency` `EUR`, `executionDate` `D+1`, `customerId` 1, `fromAccountId` 1, and `paymentId`, `reason`, `executedAt` and `cancelledAt` all `null`.

### TC-SP-02: Back from the confirmation keeps the entered data

**Covers:** UI flow · **Preconditions:** Logged in as Alice, on the Scheduled payments page.

1. Fill in a valid payment: IBAN `NL01VALU0000000003`, name `Bob de Vries`, amount `10.00`, date `D+5`, description `Back test`. Click **Review payment**.
2. Click **Back**.
3. Change only the amount to `11.00` and click **Review payment**.
4. Click **Confirm**.
   - **Verify:** the list contains a `SCHEDULED` payment to `Bob de Vries` / `NL01VALU0000000003` of `€11.00` for `D+5` with description `Back test`, and none of `€10.00`.

### TC-SP-03: Navigate from the account detail page with the account preselected

**Covers:** UI flow, R3 · **Preconditions:** Logged in as Alice.

1. On the accounts overview, open account `NL01VALU0000000002` (SAVINGS).
2. Click **Schedule a payment**.
3. Without changing **From account**, schedule a valid payment (IBAN `NL01VALU0000000003`, name `Bob`, amount `5.00`, date `D+1`) via **Review payment** and **Confirm**.
   - **Verify:** the new entry shows "From NL01VALU0000000002".
   - **Verify:** after the form is reset, **From account** is still `NL01VALU0000000002`.

### TC-SP-04: Schedule in the source account's currency (USD)

**Covers:** R4 (currency follows the account) · **Preconditions:** Logged in as Bob.

1. Open **Scheduled payments** and select **From account** `NL01VALU0000000004`.
2. Enter IBAN `NL01VALU0000000001`, name `Alice Janssen`, amount `20.00`, date `D+1`. Click **Review payment**.
3. Click **Confirm**.
   - **Verify:** the list entry shows the amount in USD (`$20.00` or the locale equivalent).
   - **Verify:** `curl -s http://localhost:8082/api/customers/2/scheduled-payments` shows `"currency":"USD"` for this entry.

### TC-SP-05: Beneficiary IBAN is normalised (spaces, lowercase)

**Covers:** IBAN handling · **Preconditions:** Logged in as Alice.

1. Enter IBAN `nl01 valu 0000 0000 03`, name `Bob`, amount `1.00`, date `D+1`. Click **Review payment**.
2. Click **Confirm**.
   - **Verify:** the list entry shows the IBAN normalised as `NL01VALU0000000003`.
   - **Verify:** the API list shows `"toAccountIban":"NL01VALU0000000003"`.

### TC-SP-06: Optional description

**Covers:** UI flow · **Preconditions:** Logged in as Alice.

1. Fill in a valid payment (IBAN `NL01VALU0000000003`, name `Bob`, amount `2.00`, date `D+1`) with the description left empty. Click **Review payment**.
2. Click **Confirm**.
   - **Verify:** the payment is scheduled (`SCHEDULED`) and its list entry shows no description line.

---

## B. Input validation and boundary values (UI)

All cases in this section: logged in as Alice, on the Scheduled payments page,
starting from an otherwise valid form (IBAN `NL01VALU0000000003`, name `Bob`,
amount `10.00`, date `D+1`) unless stated otherwise.

The table-based test cases are data-driven: run every row as a separate test.
For each row, enter the value, click **Review payment** and verify the expected
result. "Confirmation shown" means the input is accepted; an error message
means it is rejected and nothing is scheduled.

### TC-SP-10: Beneficiary IBAN boundaries

**Covers:** IBAN format (15-34 characters: 2 letters, 2 digits, 11-30 alphanumerics)

| Row | Beneficiary IBAN | Length | Expected result |
|-----|------------------|--------|-----------------|
| 1 | *(empty)* | 0 | Error "Please enter a valid IBAN (e.g. NL91ABNA0417164300)." |
| 2 | `NL01ABCD000001` | 14 | Same IBAN error (one character too short) |
| 3 | `NL01ABCD0000001` | 15 | Confirmation shown (shortest valid length) |
| 4 | `NL01ABCDEFGHIJ0123456789ABCDEFGHIJ` | 34 | Confirmation shown (longest valid length) |
| 5 | `NL01ABCDEFGHIJ0123456789ABCDEFGHIJK` | 35 | Same IBAN error (one character too long) |
| 6 | `1L01VALU0000000003` | 18 | Same IBAN error (country code not two letters) |
| 7 | `NLA1VALU0000000003` | 18 | Same IBAN error (check digits not two digits) |
| 8 | `NL01VALU-000000003` | 18 | Same IBAN error (special character) |
| 9 | `  NL01 VALU 0000 0000 03  ` | - | Confirmation shown (whitespace is ignored) |
| 10 | `nl01valu0000000003` | 18 | Confirmation shown (lowercase is accepted) |

### TC-SP-11: Amount boundaries

**Covers:** R2

| Row | Amount | Expected result |
|-----|--------|-----------------|
| 1 | *(empty)* | Error "Amount must be greater than zero." |
| 2 | `0` | Same error |
| 3 | `0.00` | Same error |
| 4 | `-0.01` | Same error |
| 5 | `-100` | Same error |
| 6 | `0.01` | Confirmation shows `€0.01` (smallest valid amount) |
| 7 | `10000.00` | Confirmation shows `€10,000.00` (fraud limit, still accepted for scheduling) |
| 8 | `10000.01` | Confirmation shows `€10,000.01`: accepted for scheduling, because the fraud limit only applies at execution |

### TC-SP-12: Amount with more than two decimals

**Covers:** R2 boundary · **Clarify**

Run once with amount `0.001` and once with `10.005`.

1. Enter the amount, click **Review payment**, and click **Confirm** if the confirmation is shown.
   - **Verify (Clarify):** record whether the payment is scheduled, the amount shown in the list, and the `amount` stored in the API list. The spec does not define precision. An amount shown as `€0.00` but accepted contradicts the intent of R2. Raise it.

### TC-SP-13: Execution date boundaries

**Covers:** R1. Type the date into the date field where the picker blocks a value.

| Row | Execution date | Expected result |
|-----|----------------|-----------------|
| 1 | *(empty)* | Error "Execution date must be in the future." |
| 2 | `D-1` | Same error |
| 3 | `D` (today) | Same error (today is not in the future) |
| 4 | `D+1` | Confirmation shown (earliest valid date) |
| 5 | `D+365` | Confirmation shown |
| 6 | 29 February of the next leap year | Confirmation shown with that date |
| 7 | 30 February (or another non-existent date) | The browser does not accept the value, or the same error |

### TC-SP-14: Far-future execution date

**Covers:** R1 boundary · **Clarify**

1. Enter execution date `31-12-9999` and click **Review payment**, then **Confirm**.
   - **Verify (Clarify):** record whether the payment is accepted. The spec defines no maximum horizon. A real bank would usually limit it (for example to one year).
   - **Verify:** if accepted, the list shows "Execution date: 31-12-9999" (in the locale format) without display errors.

### TC-SP-15: Validation order

**Covers:** UI validation logic. Validation stops at the first failing field, in the order account, IBAN, amount, date.

| Row | IBAN | Amount | Date | Expected result |
|-----|------|--------|------|-----------------|
| 1 | *(empty)* | `0` | *(empty)* | Only the IBAN error is shown |
| 2 | `NL01VALU0000000003` | `0` | *(empty)* | Only the error "Amount must be greater than zero." is shown |
| 3 | `NL01VALU0000000003` | `10.00` | *(empty)* | Only the error "Execution date must be in the future." is shown |

### TC-SP-16: Empty beneficiary name

**Covers:** Input validation · **Clarify**

1. Fill in a valid form but leave **Beneficiary name** empty. Click **Review payment**, and click **Confirm** if the confirmation is shown.
   - **Verify (Clarify):** record whether an error is shown or the payment is scheduled. The field is marked as required, but the UI does not check it. If scheduled, record how the list entry looks without a name (it is normally the entry's title).

### TC-SP-17: Double-click on Confirm

**Covers:** duplicate submission

1. Fill in a valid form (amount `3.33`, date `D+1`) and click **Review payment**.
2. Double-click **Confirm** quickly.
   - **Verify:** while the request runs, the button reads "Scheduling..." and is disabled.
   - **Verify:** exactly **one** new `€3.33` entry appears in the list (also check the API list).

### TC-SP-18: Error message disappears when a field changes

**Covers:** UI validation logic

1. Leave the IBAN empty and click **Review payment**.
2. Type a valid IBAN `NL01VALU0000000003`.
   - **Verify:** the IBAN error message disappears as soon as the field changes.

---

## D. Account ownership and access

### TC-SP-30: Only own accounts are offered

**Covers:** R3

1. Log in as `bob` and open **Scheduled payments**.
   - **Verify:** **From account** offers exactly `NL01VALU0000000003` and `NL01VALU0000000004`, and none of Alice's accounts.

### TC-SP-31: Preselecting another customer's account through the URL

**Covers:** R3

1. Logged in as Alice, open http://localhost:5173/scheduled-payments?accountId=3 directly (account 3 is Bob's).
2. Without changing **From account**, schedule a valid payment.
   - **Verify:** the entry shows "From NL01VALU0000000001" (Alice's first account), not Bob's account.

### TC-SP-32: Backend rejects another customer's source account

**Covers:** R3

1. Send a valid body with `customerId` 1 and `fromAccountId` 3.
   - **Verify:** `403`, `{"error":"Source account does not belong to this customer"}`.
   - **Verify:** no new entry for customer 1 or customer 2.
2. Send a valid body with `customerId` 2 and `fromAccountId` 3.
   - **Verify:** `201` (Bob owns account 3).

### TC-SP-33: Customers only see their own scheduled payments in the UI

**Covers:** R3, R8

1. As Alice, schedule a payment with description `Alice only`.
2. Log out, log in as Bob and schedule a payment with description `Bob only`.
3. Log out, log in as Alice and open **Scheduled payments**.
   - **Verify:** Alice's list contains `Alice only` but not `Bob only`.

### TC-SP-34: Customer ids are trusted by the backend

**Covers:** R3 · **Known risk** (authentication is deliberately fake)

1. Without logging in, run `curl -s http://localhost:8082/api/customers/1/scheduled-payments`.
   - **Verify (Known risk):** Alice's scheduled payments are returned without any credentials. Record this as a security finding against R3/R8.
2. Send a valid schedule request with `customerId` 1 and `fromAccountId` 1, without logging in.
   - **Verify (Known risk):** `201`, the payment is scheduled on Alice's account. Record it.
3. Cancel one of Alice's `SCHEDULED` payments with `?customerId=1`, without logging in.
   - **Verify (Known risk):** `200`, status `CANCELLED`. Record it.

### TC-SP-35: Scheduled payments page requires login

1. Log out. Open http://localhost:5173/scheduled-payments directly.
   - **Verify:** you are redirected to the login page, and no scheduled payment data is shown.

### TC-SP-36: Admin has no Scheduled payments link

1. Log in as `admin` / `admin123`.
   - **Verify:** the header shows **no** "Scheduled payments" link.

### TC-SP-37: Admin opens the Scheduled payments page directly

**Clarify**

1. Log in as `admin` / `admin123` and open http://localhost:5173/scheduled-payments directly.
   - **Verify (Clarify):** record what happens. Expected: the admin has no accounts, so the form has no source accounts and **Review payment** is disabled. Should admins be able to reach this page at all?

---

## E. Scheduled payments list

### TC-SP-40: Empty list

**Covers:** R8 · **Preconditions:** Payments Service freshly restarted.

1. Log in as Alice and open **Scheduled payments**.
   - **Verify:** the right card shows "No scheduled payments yet."
   - **Verify:** `curl -s http://localhost:8082/api/customers/1/scheduled-payments` returns `[]`.

### TC-SP-41: Ordering by execution date, then by creation

**Covers:** R8 · **Preconditions:** Alice has no scheduled payments.

1. Schedule, in this order: amount `3.00` on `D+3`, amount `1.00` on `D+1`, amount `2.00` on `D+2`, amount `4.00` on `D+1`.
2. Reload the page (F5).
   - **Verify:** the order is `1.00` (`D+1`), `4.00` (`D+1`), `2.00` (`D+2`), `3.00` (`D+3`). That's ascending date, and for the same date, the earliest created first.

### TC-SP-42: Entries in every status remain visible

**Covers:** R8, R10

1. Make sure Alice has one payment in each status. Schedule four payments for `D+1`, then:
   - leave the first `SCHEDULED`,
   - cancel the second (`CANCELLED`),
   - execute the third, a valid payment of `€1.00` from account 1, with the test-only endpoint (`EXECUTED`),
   - execute the fourth, `10000.01` from account 2, with the test-only endpoint (`FAILED`, fraud limit).

   Open **Scheduled payments**.
   - **Verify:** all four are listed with their status label.
   - **Verify:** only the `SCHEDULED` entry has a **Cancel** button.
   - **Verify:** only the `FAILED` entry shows a reason line.
   - **Verify (Clarify):** past and future payments are mixed in one list. Is that acceptable for the customer?

### TC-SP-43: List when the Accounts Service is down

**Covers:** R8, resilience · **Preconditions:** Alice has at least one scheduled payment. Start the Accounts Service again afterwards.

1. Stop the Accounts Service. Open **Scheduled payments** as Alice.
   - **Verify:** an accounts error banner is shown in the left card, and **Review payment** is disabled.
   - **Verify:** the right card still lists Alice's scheduled payments. The source is shown as "From account 1" instead of the IBAN.

---

## F. Cancelling

### TC-SP-50: Cancel a scheduled payment

**Covers:** R5 · **Preconditions:** Alice has a `SCHEDULED` payment of `€7.00` for `D+2`.

1. On Alice's Scheduled payments page, click **Cancel** on the `€7.00` entry.
   - **Verify:** the entry shows status `CANCELLED` and has no **Cancel** button (it can't be cancelled again).
2. Run the API list for customer 1.
   - **Verify:** the entry has `"status":"CANCELLED"`, a `cancelledAt` timestamp, and `executedAt` and `paymentId` set to `null`.
3. Check Alice's balance and payment history for the source account via the API.
   - **Verify:** the balance is unchanged and no payment was recorded.

### TC-SP-51: A cancelled payment is never executed

**Covers:** R5, R7

1. Take the cancelled payment from TC-SP-50 and run the test-only execute endpoint for its id.
   - **Verify:** `409`, `{"error":"Scheduled payment is CANCELLED and can no longer be executed"}`.
   - **Verify:** the status remains `CANCELLED`.
   - **Verify:** the balance and payment history are unchanged.

### TC-SP-52: Cancel twice

**Covers:** R5

1. Cancel the payment from TC-SP-50 again via the API (`?customerId=1`).
   - **Verify:** `409`, `{"error":"Scheduled payment is CANCELLED and can no longer be cancelled"}`.
   - **Verify:** `cancelledAt` is unchanged.

### TC-SP-53: Cancel another customer's or a non-existent payment

**Covers:** R5, R3

1. Take a `SCHEDULED` payment of Alice and cancel it via the API with `?customerId=2` (Bob).
   - **Verify:** `404`, `{"error":"Scheduled payment {id} not found"}`. It doesn't reveal that the payment exists.
   - **Verify:** Alice's payment is still `SCHEDULED`.
2. Cancel id `999999` with `?customerId=1`.
   - **Verify:** `404`, `{"error":"Scheduled payment 999999 not found"}`.
3. Cancel without the `customerId` parameter.
   - **Verify:** `400`. **Clarify:** record the error body format.

### TC-SP-54: Cannot cancel after successful execution

**Covers:** R6 · **Preconditions:** An `EXECUTED` payment of Alice: schedule a valid payment of `€1.00` from account 1 for `D+1` and execute it with the test-only endpoint.

1. Open Alice's Scheduled payments page.
   - **Verify:** the `EXECUTED` entry has no **Cancel** button.
2. Cancel it via the API with `?customerId=1`.
   - **Verify:** `409`, `{"error":"Scheduled payment is EXECUTED and can no longer be cancelled"}`.
   - **Verify:** the status remains `EXECUTED`, and the balance and history are unchanged.

### TC-SP-55: Cannot cancel after failed execution

**Covers:** R6 · **Clarify** · **Preconditions:** A `FAILED` payment of Alice: schedule `10000.01` from account 2 for `D+1` and execute it with the test-only endpoint (it fails on the fraud limit).

1. Open Alice's Scheduled payments page.
   - **Verify:** the `FAILED` entry has no **Cancel** button.
2. Cancel it via the API.
   - **Verify:** `409`, `{"error":"Scheduled payment is FAILED and can no longer be cancelled"}`.
   - **Verify (Clarify):** the spec says "cannot be cancelled after execution". Does a failed attempt count as execution?

### TC-SP-56: Cancel from a stale page after execution

**Covers:** R6, UI handling of a race

1. As Alice, schedule a payment of `€4.00` for `D+1` and keep the page open. Do **not** reload.
2. In a terminal, execute it with the test-only endpoint.
   - **Verify:** `200`, `"status":"EXECUTED"`.
3. Back in the (stale) browser page, click **Cancel** on that entry.
   - **Verify:** a red banner shows "Scheduled payment is EXECUTED and can no longer be cancelled".
   - **Verify:** the list reloads and the entry now shows `EXECUTED` without a **Cancel** button.
   - **Verify:** Alice's balance is debited exactly once (`€4.00`).

---

## H. Balance and fraud boundaries at execution

Do a full data reset before each test case in this section. All payments are
scheduled for `D+1` and executed with the test-only endpoint. Check statuses,
reasons, balances and payment history via the API, unless a step says otherwise.

### TC-SP-70: Fraud limit boundary (10000.01 and 10000.00)

**Covers:** R4

1. From Alice's savings (account 2, `11000.00`), schedule `10000.01` and execute it.
   - **Verify:** `FAILED`, "Amount exceeds maximum allowed per transaction (10000)". The balance stays `11000.00`. History shows `REJECTED` `10000.01` with the same reason.
2. From account 2, schedule `10000.00` and execute it.
   - **Verify:** `EXECUTED`. The balance is `1000.00`. History shows `COMPLETED` `10000.00`.

### TC-SP-71: SAVINGS cannot go below zero

**Covers:** R4

1. From account 2, schedule `10000.00` and execute it.
   - **Verify:** `EXECUTED`, balance `1000.00`.
2. Schedule `1000.01` from account 2 and execute it.
   - **Verify:** `FAILED`, "Insufficient funds". The balance stays `1000.00`.
3. Schedule `1000.00` from account 2 and execute it.
   - **Verify:** `EXECUTED`, balance exactly `0.00`.
4. Schedule `0.01` from account 2 and execute it.
   - **Verify:** `FAILED`, "Insufficient funds". The balance stays `0.00`.

### TC-SP-72: CHECKING overdraft limit boundary

**Covers:** R4

1. From Alice's checking (account 1, `2500.00`), schedule `7500.01` and execute it.
   - **Verify:** `FAILED`, "Insufficient funds", balance `2500.00`.
2. Schedule `7500.00` from account 1 and execute it.
   - **Verify:** `EXECUTED`, balance exactly `-5000.00`.
3. Schedule `0.01` from account 1 and execute it.
   - **Verify:** `FAILED`, "Insufficient funds", balance `-5000.00`.
4. Log in as Alice and open account `NL01VALU0000000001`.
   - **Verify:** the account detail page shows the balance as `-€5,000.00` (or the locale equivalent).

### TC-SP-73: Several payments due on the same day compete for funds

**Covers:** R4, R7 · **Clarify**

1. As Bob, from account 3 (`500.00`, limit `-5000.00`), schedule in this order: A `3000.00` for `D+1`, then B `3000.00` for `D+1`.
2. Execute A, then B.
   - **Verify:** A is `EXECUTED` and the balance is `-2500.00`.
   - **Verify:** B is `FAILED`, "Insufficient funds", and the balance stays `-2500.00`.
3. **Clarify:** the background run executes same-day payments in creation order. The customer can't set a priority. Is that acceptable?

---

## I. Executed exactly once

### TC-SP-80: Execute twice

**Covers:** R7

1. As Alice, schedule a valid payment of `€9.00` for `D+1` and execute it.
   - **Verify:** `EXECUTED`, and the balance is debited by `9.00`.
2. Execute the same id again.
   - **Verify:** `409`, `{"error":"Scheduled payment is EXECUTED and can no longer be executed"}`.
   - **Verify:** the balance is unchanged since step 1.
   - **Verify:** payment history contains exactly **one** `9.00` payment.

### TC-SP-81: Execute a non-existent payment

**Covers:** R7

1. Execute id `999999`.
   - **Verify:** `404`, `{"error":"Scheduled payment 999999 not found"}`.

### TC-SP-82: Parallel execute and cancel

**Covers:** R6, R7

1. As Alice, schedule a valid payment of `€8.00` for `D+1`. Note the id.
2. In two terminals, at the same moment, run the execute call and the cancel call (`?customerId=1`) for that id. Repeat with new payments a few times.
   - **Verify:** each time, exactly one call succeeds (`200`) and the other returns `409`.
   - **Verify:** the final status is either `EXECUTED` (with exactly one debit and one history entry) or `CANCELLED` (with no debit and no history entry), never both.
