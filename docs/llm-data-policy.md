# What (not) to send to an LLM: the E2E failure analysis

The CI job `analyze-e2e-failures` has Claude analyse failed Playwright tests. This
document records which data that LLM may see, and how the pipeline enforces it. It
is also workshop material: every measure below is an example of a step you can take
in your own pipeline.

The core rule: **control what the model can *reach*, not just what you *send*.** An
agent with file tools reads whatever is in its working directory.

## 1. Inventory: what could the LLM reach?

| Source | Before | Now |
|---|---|---|
| Repository checkout, including `.git/` (history, and the `GITHUB_TOKEN` that `actions/checkout` stores in `.git/config` by default) | readable | not reachable: Claude runs in a separate directory outside the checkout; checkout without stored credentials |
| Source code, docs, `CLAUDE.md` | everything | allowlisted directories only, pseudonymized |
| Config files (`src/main/resources`, `.env`) | readable | not included |
| Error contexts (error, call log, page snapshot) | readable | failing attempts only, pseudonymized |
| Full Playwright JSON report | readable | not included; a pseudonymized summary (`failures.json`) instead |
| Traces, screenshots, HTML report (network data, cookies, `localStorage`) | readable | not included |
| Service logs | complete | only a time window around each failing attempt, pseudonymized |
| Environment variables, other secrets | not readable (no Bash), but present in the job | only the API key, only in the step that calls Claude |

## 2. Classification

| Data | Class | Decision |
|---|---|---|
| Tokens, API keys, passwords, cookies, `Authorization` headers, private keys | secret | never. Redacted where recognized; the secret scan stops the analysis if anything is left |
| Seeded accounts `NL01VALU0000000001` to `...0004`, logins `alice`, `bob`, `admin` and their passwords | synthetic test data | allowed, not redacted (needed to connect tests, data and errors) |
| Other IBANs, email addresses | personal data in real systems | after pseudonymization only |
| Customer names | personal data in real systems | the seeded names are synthetic; see "Known limitations" |
| Application and test source code | confidential | allowed for this workshop (decision); pseudonymized like the data |

In a real project, check whether your "test data" is really synthetic. Copies of
production data in test environments are common, and this pipeline would then send
real customer data without anyone noticing.

## 3. Legal and contractual basis

Fill this in before sending anything. Technical measures reduce risk; they do not give permission.

| Question | Answer |
|---|---|
| Vendor terms: is data retained, and for how long? | |
| Is data used for model training? | |
| Processing region | |
| Zero data retention available / needed? | |
| Data processing agreement in place? | |
| Internal policy that applies | |
| Approved by, and when | |

## 4–10. How the pipeline enforces it

| Step | Measure | Where |
|---|---|---|
| 4. Allowlist | Only listed files are copied into `$RUNNER_TEMP/analysis-workspace`; Claude runs there | `.github/scripts/prepare-analysis-workspace.mjs` |
| 5. Secrets out of reach | `persist-credentials: false`; API key on one step; deny rules for `.git`, `.env*`, keys, home directory | `.github/workflows/ci.yml`, `.github/claude/analysis-settings.json` |
| 6. Redaction | Consistent placeholders (`<IBAN_1 len=15>`, `<EMAIL_1>`, `<TOKEN_1>`, `<SECRET_1>`) in data *and* source, one mapping per run, never stored | `.github/scripts/redact.mjs` |
| 7. Gate | gitleaks (pinned, checksum-verified) scans the workspace; any finding: nothing is sent, the job fails | `.github/scripts/scan-secrets.sh` |
| 7. Canary | Unit tests with planted fake secrets; before each scan, a random fake key must be detected | `.github/scripts/redact.test.mjs`, the canary step in `ci.yml` |
| 8. Prevention at the source | See below | |
| 9. Output is data too | Model text is escaped in the report; the report is scanned before it is published; raw results are kept 14 days, the report 90 | `render-e2e-analysis.mjs`, `ci.yml` |
| 10. Auditable | `manifest.json`: every file the LLM could read, with source, SHA-256, size and redaction counts | published with the report |

### Step 8: findings at the source

- The services don't log request bodies or headers, and `show-sql` is off.
- The tests don't print data to the console.
- Test credentials are the seeded, synthetic logins, in the test code. In a real
  project they would come from CI secrets, never from code.
- Traces (`trace: 'on-first-retry'`) do contain network data and `localStorage`.
  They are not sent to the LLM, and the raw artifacts are kept for 14 days only.

## Known limitations

These are deliberate discussion points, not oversights:

- **Patterns only catch what has a recognizable shape.** Names (`Bob de Vries`,
  or a typed name in a form) are not redacted: no pattern can recognize a name.
  That needs a maintained list of sensitive values, and lists go out of date.
- **Over-redaction.** The example IBAN in the IBAN error message
  (`NL91ABNA0417164300`) is IBAN-shaped and not seeded, so it is redacted too, in the
  frontend code and in the test that checks the message. Consistent, harmless, and a
  reminder that a pattern doesn't know whether a value is sensitive.
- **Near-misses leak.** The 35-character test value `NL01ABCDEFGHIJ0123456789ABCDEFGHIJK`
  is not IBAN-shaped, so it is not redacted, but its first 34 characters are the
  redacted 34-character IBAN. Values derived from a sensitive value can reveal it.
- **The scanner has gaps too.** It catches known secret formats (a private key in a
  log stops the analysis even though the redaction doesn't know private keys), but
  not everything. That's why minimization (steps 4 and 8) comes before redaction.
- **The deny rules in `analysis-settings.json` are defence in depth.** The main
  protection is the separate workspace: what isn't copied there can't be read.

## How to verify

- `node --test .github/scripts/redact.test.mjs` runs the redaction tests.
- `manifest.json` in the `e2e-failure-analysis` artifact shows exactly what the LLM
  could read in that run, and how much was redacted per file.
- `e2e-secret-scans` contains the scan results (rule ids and locations only).
