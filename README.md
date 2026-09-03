# ValuBank

A deliberately small online-banking system used in my [Valuable Feedback, Fast](https://www.ontestautomation.com/training/valuable-feedback-fast/) course. It is a
monorepo with a React frontend and four independent Spring Boot backend
services.

```
valubank/
├── frontend/                     React (Vite) customer-facing web app
├── services/
│   ├── accounts-service/         Owns customer accounts, balances, interest lookup, login
│   ├── payments-service/         Owns payments/transfers
│   ├── fraud-service/            Simple rule-based fraud checks (no DB)
│   └── interest-rate-service/    Interest-rate configuration store
└── scripts/
    ├── start-all.ps1             Start everything (Windows / PowerShell)
    └── start-all.sh              Start everything (bash)
```

## Architecture

```
                        ┌─────────────┐
                        │  Frontend   │  React, localhost:5173
                        └──────┬──────┘
                      login /  │  \ payments
                    accounts   │   \
                               ▼    ▼
                 ┌─────────────┐   ┌──────────────────┐
                 │  Accounts   │◄──┤     Payments     │
                 │  Service    │   │     Service      │
                 │  :8081      │   │     :8082        │
                 └──────┬──────┘   └─────────┬────────┘
                        │                    │
                        ▼                    ▼
              ┌──────────────────┐   ┌────────────────┐
              │ Interest Rate /  │   │  Fraud Service │
              │ Config Service   │   │  :8083         │
              │ :8084            │   └────────────────┘
              └──────────────────┘
```

Each backend service owns its own database (H2, in-memory) — no service
reaches into another service's data store directly. Cross-service calls go
over plain HTTP.

**Known, intentional weakness:** the Payments → Accounts Service call
(`POST /api/accounts/{id}/balance-mutations`, `GET /api/accounts/{id}`) has
**no formal API contract** — no OpenAPI spec, no shared/generated client, no
schema validation. Each service hand-rolls its own copy of the request/response
shape. This is deliberate: it's a discoverable weak point for workshop
participants to find and address (e.g. by introducing a contract test, an
OpenAPI spec, or a shared schema).

## Prerequisites

- JDK 17+
- Maven 3.9+ (`mvn` on your PATH)
- Node.js 18+ and npm

## Running everything at once

**Windows / PowerShell:**
```powershell
.\scripts\start-all.ps1
```
Opens one PowerShell window per service plus the frontend. Close a window to
stop that service.

**bash (macOS/Linux/WSL/Git Bash):**
```bash
./scripts/start-all.sh
```
Runs everything in the background, logs to `./logs/<service>.log`. Ctrl+C
stops everything.

Once running:

| Component              | URL                    |
|------------------------|------------------------|
| Frontend               | http://localhost:5173  |
| Accounts Service       | http://localhost:8081  |
| Payments Service       | http://localhost:8082  |
| Fraud Service          | http://localhost:8083  |
| Interest Rate Service  | http://localhost:8084  |

Start order matters a little in practice (Accounts Service calls Interest
Rate Service; Payments Service calls Accounts Service and Fraud Service) but
every service tolerates its dependencies being down or starting late — it
just returns an error until the dependency is reachable, rather than crashing.

## Running services individually

Each backend service is a standalone Maven project:

```bash
cd services/accounts-service
mvn spring-boot:run
```

Same pattern for `payments-service`, `fraud-service`, and
`interest-rate-service`. Each has its own `application.yml` with its port
already set, and seeds its own H2 in-memory database on startup — no shared
setup required. H2 web consoles are available at `/h2-console` on each
DB-owning service (Accounts, Payments, Interest Rate) while it's running.

The frontend runs independently too:

```bash
cd frontend
npm install
npm run dev
```

By default it points at `http://localhost:8081` (Accounts) and
`http://localhost:8082` (Payments). Override with a `.env` file — see
`frontend/.env.example` — if you're running services on different hosts/ports.

## Seeded data

### Customers / login (Accounts Service)

| Username | Password      | Full name         |
|----------|---------------|-------------------|
| alice    | password123   | Alice Janssen     |
| bob      | password123   | Bob de Vries      |

Login is intentionally simple (plaintext password check, no JWT/session
tokens) — it exists to give the workshop a login screen, not to demonstrate
production authentication.

### Accounts (Accounts Service)

| Owner | IBAN                     | Type     | Balance   | Currency |
|-------|--------------------------|----------|-----------|----------|
| alice | NL01VALU0000000001       | CHECKING | 2,500.00  | EUR      |
| alice | NL01VALU0000000002       | SAVINGS  | 10,000.00 | EUR      |
| bob   | NL01VALU0000000003       | CHECKING | 500.00    | EUR      |

### Interest rates (Interest Rate / Configuration Service)

| Account type | Rate  |
|--------------|-------|
| CHECKING     | 0.1%  |
| SAVINGS      | 1.5%  |

Rates can be changed at runtime via `PUT /api/interest-rates/{accountType}`
on the Interest Rate Service — Accounts Service picks up the new value on its
next lookup, with no restart needed.

### Fraud rules (Fraud Service — hardcoded, no DB)

- Payments over **10,000** are rejected ("Amount exceeds maximum allowed per
  transaction (10000)").
- Payments to IBAN **NL99BLOCKED0000000** are rejected ("Destination account
  is flagged for fraud").
- Everything else is approved.

### Payments (Payments Service)

No seed data — the Payments DB starts empty and fills up as you use the app.

## Payment flow (what happens on "make a payment")

1. Frontend calls `POST /api/payments` on the Payments Service.
2. Payments Service fetches the source account from Accounts Service and
   checks the balance.
3. Payments Service calls the Fraud Service to check the payment.
4. If approved and funds are sufficient, Payments Service calls Accounts
   Service to debit the account.
5. Payments Service records the outcome (`COMPLETED`, `REJECTED`, or
   `FAILED`, with a reason where applicable) and returns it to the frontend.

Try a payment over 10,000, or to `NL99BLOCKED0000000`, to see a rejection;
try stopping the Accounts Service mid-demo to see a `FAILED` payment and the
frontend's error handling.
