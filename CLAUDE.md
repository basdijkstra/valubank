# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

ValuBank is a deliberately small online-banking system used as teaching material in a testing workshop ("Valuable Feedback, Fast"). It is a monorepo: a React (Vite) frontend, four independent Spring Boot 3.5 / Java 21 services (each its own Maven project, no parent POM), and a Playwright E2E suite. Several weaknesses are **intentional** so workshop participants can discover them. Don't "fix" them unless asked (see below).

## Commands

Each service is built and run on its own, from its own directory (`services/<name>`):

```bash
mvn spring-boot:run                                    # run one service
mvn test                                               # all tests of that service
mvn test -Dtest=ScheduledPaymentServiceTest            # one test class
mvn test -Dtest=ScheduledPaymentServiceTest#rejectsZeroAmount   # one test method
```

Everything at once: `scripts/start-all.ps1` (one window per service) or `scripts/start-all.sh` (background, logs in `logs/<service>.log`). To stop: `scripts/stop-all.ps1` or `scripts/stop-all.sh`, which kill whatever is listening on ports 8081-8084 and 5173.

Frontend (`frontend/`): `npm run dev`, or `npm run build` (which also catches JSX errors). There is no lint or unit test setup for the frontend.

E2E (`e2e/`), which needs **all** services and the frontend running:

```bash
npx playwright test                                    # all tests
npx playwright test tests/payment.spec.ts              # one file
npx playwright test -g "incorrectly formatted IBAN"    # tests by title
npx playwright test --list                             # compile/list without running
```

CI (`.github/workflows/ci.yml`) compiles every service in `services/*/` on JDK 21 through 26, starts everything, then runs `mvn clean test` per service and the Playwright suite. It discovers services and their ports from each `application.yml` (`server.port`), so a new service needs no CI change.

## Architecture

| Component | Port | Owns |
|-----------|------|------|
| frontend | 5173 | UI; calls Accounts and Payments directly from the browser |
| accounts-service | 8081 | customers, login, accounts, balances, interest application |
| payments-service | 8082 | payments, scheduled payments; orchestrates the payment flow |
| fraud-service | 8083 | hardcoded fraud rules (`FraudRuleEngine`), no DB |
| interest-rate-service | 8084 | interest rate per account type |

- **Each service has its own in-memory H2 database**, seeded on startup (accounts: `DataSeeder`; interest rates: `InterestRateSeeder`; payments starts empty). Restarting a service resets its data. Seeded logins: `alice`/`password123`, `bob`/`password123`, `admin`/`admin123`. The README has the full seed table.
- **Service-to-service calls are plain `RestTemplate` HTTP** with hand-written DTOs duplicated in each service (for example `payments/dto/AccountDto` mirrors `accounts/dto/AccountDto`). There is deliberately **no shared contract**, no OpenAPI and no shared client. Changing a cross-service JSON shape means editing both sides by hand. Services tolerate their dependencies being down: they return errors instead of crashing.
- **Auth is intentionally fake.** Login returns a profile that the frontend keeps in `localStorage` (`AuthContext`). Endpoints trust a `customerId` sent by the client, and the admin role is only enforced in the frontend (`RequireAuth adminOnly`).
- **Payment flow** (`PaymentService.createPayment`): verify the source account through Accounts (if that fails: 502, nothing saved), then the fraud check, then `POST /api/accounts/{id}/balance-mutations` to debit. The outcome is always saved as a `Payment` with status `COMPLETED`, `REJECTED` or `FAILED` plus a reason. Business rejections still return 201; only HTTP-level problems throw in the frontend.
- **Balance rules live in accounts-service** (`AccountService.applyBalanceMutation`): CHECKING may go down to -5000, SAVINGS not below 0; otherwise 409 (insufficient funds).
- **Scheduled payments** (payments-service): `ScheduledPaymentService` validates the date, amount, IBAN and ownership when scheduling. At execution it reuses `PaymentService.createPayment`, so all existing payment rules apply and the result shows up in payment history. Statuses: `SCHEDULED`, `EXECUTED`, `FAILED`, `CANCELLED`. `ScheduledPaymentRunner` runs due payments on the cron `valubank.scheduled-payments.cron` and once at startup. "Today" comes from an injectable `Clock` bean. There is a test-only `POST /api/test-support/scheduled-payments/{id}/execute` (`TestSupportController`), present only while `valubank.test-support.enabled=true`. Spec: `docs/scheduled-payments-spec.md`.
- **IBAN validation** is structural only (regex, no mod-97), because the seeded IBANs are fictional. Immediate payments check it only in the frontend (`frontend/src/utils/iban.js`); scheduled payments also check it in the backend with the same pattern.
- **Error responses** use `{"error": "..."}`, mapped in each service's `@RestControllerAdvice`. The frontend `api/*.js` wrappers show `body.error`.

## Conventions

- Java: constructor injection, explicit getters and setters (no Lombok), status values as `String` constants, one exception class per error case mapped in `GlobalExceptionHandler`. Javadoc explains *why*, often pointing to the workshop context.
- Backend unit tests use plain Mockito with constructor-built services (see `ScheduledPaymentServiceTest`); `*ApplicationTests` only check that the context loads.
- E2E tests use page objects in `e2e/tests/pages/` and mostly select by accessible label or role (`getByLabel`, `getByRole`), so keep form labels stable when changing the UI.
- Document user-visible behaviour and new endpoints in `README.md`, which serves as the workshop handout.
