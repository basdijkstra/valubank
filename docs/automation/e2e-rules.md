# E2E test rules and guidelines

Follow these rules for new tests.

## Structure
- Each spec file covers one feature, in `e2e/tests/<feature>.spec.ts`, with a kebab-case name (`add-interest.spec.ts`).
- Import with `import { test, expect } from '@playwright/test';`, then the page objects, using relative paths without an extension (`'./pages/loginPage'`).
- Tests must be able to run in parallel (`fullyParallel: true`). Run against `baseURL` `http://localhost:5173`, with Chromium only.

## Naming
- Test titles are full sentences describing the expected behaviour, e.g. `'Entering an incorrectly formatted IBAN should show a validation error and block submission'`.
- Data-driven titles interpolate the data: `` `User ${username} can login with valid credentials` ``.
- Variables are camelCase and named after the page object (`loginPage`, `accountDetailsPage`) or the value (`initialBalance`, `updatedBalance`).

## Page objects
- One class per page or screen in `e2e/tests/pages/<name>Page.ts`, with a camelCase file name and a PascalCase class (`AccountDetailsPage`).
- Constructor: `constructor(page: Page)`, stored as `private readonly page: Page`.
- Locators that tests assert on are `readonly` public fields, set in the constructor and named `<purpose>Locator` (e.g. `errorMessageLocator`). Locators used only inside the class stay inline in its methods.
- Methods are `async` and named after user intent with verbs: `open()`, `loginAs()`, `makePayment()`, `gotoAccountDetails()`, `addInterestToAccount()`, `logout()`. Getters return a value: `getAccountBalance(iban): Promise<string>`.
- One method does a whole user task, including the submit click. Parameters are plain strings in the order a user fills in the form.
- Page objects contain **no assertions**. They may wait for a state they depend on (`locator.waitFor({ state: 'visible' })`).
- Create page objects in the test with `new XxxPage(page)`. Inline them when used once (`await new AccountsOverviewPage(page).gotoAccountDetails(iban)`).

## Locators (in order of preference)
1. `getByLabel('<visible label>')` for form fields, and `getByRole('button', { name: '<text>' })` for buttons.
2. CSS id/class for elements without accessible names (`'.app-nav-greeting'`, `'#payment-to-iban + .field-error'`).
3. XPath anchored on visible data (usually the IBAN) for cards and table rows, prefixed with `xpath=`.

Keep form labels and button texts in the UI stable: tests depend on them.

## Test data
- Use the seeded data (see README): users `alice` / `bob` / `admin`, with IBANs `NL01VALU000000000x`. Write them as literals in the test or in a `testdata` array.
- Data-driven tests: declare `const testdata = [{ ... }, ...]` at the top of the spec, then `for (const { a, b } of testdata) { test(...) }`.
- When a test changes shared state (balances), read the value before acting and assert relative to it. Never assert absolute balances.
- For money calculations, use `currency.js` (`currency(x).multiply(...)`), never floating-point arithmetic.

## Assertions
- Assertions belong in the test only, and use web-first `await expect(locator)...` matchers: `toBeVisible()`, `toHaveText()`, `toContainText()`, `toHaveCount(0)`.
- For messages, check visibility first, then the text: `toHaveText` for exact UI messages (`'REJECTED - Insufficient funds'`), and `toContainText` when only a key phrase matters.
- Check for absence with `toHaveCount(0)`, not `not.toBeVisible()`.
- Plain values use `expect(value).toEqual(...)`.
- Add a short comment when an assertion's *reason* isn't obvious (for example, why no banner should appear).

## Style
- Spec files: 2-space indentation and single quotes. Page objects: 4-space indentation and double quotes. Match the file you're editing.
- Use a blank line between arrange (login, navigation), act and assert.
