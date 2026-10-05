import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from './pages/accountDetailsPage';
import { AdminPage } from './pages/adminPage';
import { ScheduledPaymentsPage } from './pages/scheduledPaymentsPage';
import { PaymentsApi } from './helpers/paymentsApi';
import { isoDate, displayDate, nextLeapDay } from './helpers/dates';
import { uniqueId, cleanUpScheduledPayments } from './helpers/testData';
import { failAccountsRequests, delayScheduleRequest } from './helpers/browserMocks';

// Test cases: docs/test-cases/scheduled-payments.md. Tests that execute payments or
// check balances are in scheduled-payment-balances.spec.ts, which runs serially.

// displayDate() and the expected amount formats assume the en-US locale.
test.use({ locale: 'en-US' });

let runId: string;

test.beforeEach(() => {
  runId = uniqueId();
});

test.afterEach(async ({ request }) => {
  await cleanUpScheduledPayments(request, runId);
});

const IBAN_ERROR = 'Please enter a valid IBAN (e.g. NL91ABNA0417164300).';
const AMOUNT_ERROR = 'Amount must be greater than zero.';
const DATE_ERROR = 'Execution date must be in the future.';

// ---------- A. Scheduling via the UI: main flow ----------

test('TC-SP-02: Going back from the confirmation should keep the entered data', async ({ page }) => {

  const description = `Back test ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.reviewPayment('NL01VALU0000000003', 'Bob de Vries', '10.00', isoDate(5), description);
  await scheduledPaymentsPage.goBack();
  await scheduledPaymentsPage.reviewWithAmount('11.00');
  await scheduledPaymentsPage.confirmPayment();

  // One entry with this description means no 10.00 payment was scheduled as well.
  const entry = scheduledPaymentsPage.entryLocatorFor(description);
  await expect(entry).toHaveCount(1);
  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('SCHEDULED');
  await expect(entry).toContainText('Bob de Vries');
  await expect(entry).toContainText('NL01VALU0000000003');
  await expect(entry).toHaveText('€11.00');
  await expect(entry).toContainText(`Execution date: ${displayDate(isoDate(5))}`);
});

test('TC-SP-03: Scheduling from the account detail page should use that account as the source', async ({ page }) => {

  const description = `Preselected ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000002');
  await new AccountDetailsPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', 'Bob', '5.00', isoDate(1), description);

  await expect(scheduledPaymentsPage.entryLocatorFor(description)).toContainText('From NL01VALU0000000002');
  // The option value is the account id: 2 is NL01VALU0000000002.
  await expect(scheduledPaymentsPage.fromAccountLocator).toHaveValue('2');
});

test('TC-SP-04: A payment scheduled from a USD account should be in USD', async ({ page, request }) => {

  const description = `USD payment ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('bob', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.selectFromAccount('NL01VALU0000000004');
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000001', 'Alice Janssen', '20.00', isoDate(1), description);

  await expect(scheduledPaymentsPage.entriesLocator.last()).toContainText('$20.00');

  const scheduledPayments = await new PaymentsApi(request).findScheduledPayments(2, description);
  expect(scheduledPayments[0].currency).toEqual('USD');
});

test('TC-SP-05: A beneficiary IBAN with spaces and lowercase letters should be stored normalised', async ({ page, request }) => {

  const description = `Normalised IBAN ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.schedulePayment('nl01 valu 0000 0000 03', 'Bob', '1.00', isoDate(1), description);

  await expect(scheduledPaymentsPage.entryLocatorFor(description)).toContainText('NL01VALU0000000003');

  const scheduledPayments = await new PaymentsApi(request).findScheduledPayments(1, description);
  expect(scheduledPayments[0].toAccountIban).toEqual('NL01VALU0000000003');
});

test('TC-SP-06: A payment without a description should be scheduled without a description line', async ({ page }) => {

  // No description to find the entry by, so the beneficiary name is unique instead.
  const recipientName = `Bob ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', recipientName, '2.00', isoDate(1), '');

  await expect(scheduledPaymentsPage.statusLocatorFor(recipientName)).toHaveText('SCHEDULED');
  await expect(scheduledPaymentsPage.descriptionLocatorFor(recipientName)).toHaveCount(0);
});

// ---------- B. Input validation and boundary values (UI) ----------
// Each row starts from a valid form (IBAN NL01VALU0000000003, name Bob, amount 10.00,
// date D+1) and changes one value. An accepted value shows the confirmation; a rejected
// one shows the error and no confirmation.

const ibanTestdata = [
  { label: 'empty', iban: '', error: IBAN_ERROR },
  { label: 'NL01ABCD000001 (14 characters)', iban: 'NL01ABCD000001', error: IBAN_ERROR },
  { label: 'NL01ABCD0000001 (15 characters)', iban: 'NL01ABCD0000001', error: null },
  { label: 'NL01ABCDEFGHIJ0123456789ABCDEFGHIJ (34 characters)', iban: 'NL01ABCDEFGHIJ0123456789ABCDEFGHIJ', error: null },
  { label: 'NL01ABCDEFGHIJ0123456789ABCDEFGHIJK (35 characters)', iban: 'NL01ABCDEFGHIJ0123456789ABCDEFGHIJK', error: IBAN_ERROR },
  { label: '1L01VALU0000000003 (country code not two letters)', iban: '1L01VALU0000000003', error: IBAN_ERROR },
  { label: 'NLA1VALU0000000003 (check digits not two digits)', iban: 'NLA1VALU0000000003', error: IBAN_ERROR },
  { label: 'NL01VALU-000000003 (special character)', iban: 'NL01VALU-000000003', error: IBAN_ERROR },
  { label: 'with surrounding and inner whitespace', iban: '  NL01 VALU 0000 0000 03  ', error: null },
  { label: 'nl01valu0000000003 (lowercase)', iban: 'nl01valu0000000003', error: null }
]

for (const { label, iban, error } of ibanTestdata) {
  test(`TC-SP-10: Beneficiary IBAN ${label} should be ${error ? 'rejected' : 'accepted'}`, async ({ page }) => {

    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs('alice', 'password123');
    await new AccountsOverviewPage(page).gotoScheduledPayments();

    const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
    await scheduledPaymentsPage.reviewPayment(iban, 'Bob', '10.00', isoDate(1), `IBAN ${runId}`);

    if (error) {
      await expect(scheduledPaymentsPage.formErrorLocator).toBeVisible();
      await expect(scheduledPaymentsPage.formErrorLocator).toHaveText(error);
      await expect(scheduledPaymentsPage.confirmationLocator).toHaveCount(0);
    } else {
      await expect(scheduledPaymentsPage.confirmationLocator).toBeVisible();
    }
  });
}

const amountTestdata = [
  { label: 'empty', amount: '', error: AMOUNT_ERROR, shownAs: null },
  { label: '0', amount: '0', error: AMOUNT_ERROR, shownAs: null },
  { label: '0.00', amount: '0.00', error: AMOUNT_ERROR, shownAs: null },
  { label: '-0.01', amount: '-0.01', error: AMOUNT_ERROR, shownAs: null },
  { label: '-100', amount: '-100', error: AMOUNT_ERROR, shownAs: null },
  { label: '0.01', amount: '0.01', error: null, shownAs: '€0.01' },
  { label: '10000.00 (fraud limit)', amount: '10000.00', error: null, shownAs: '€10,000.00' },
  // The fraud limit only applies at execution, not when scheduling.
  { label: '10000.01 (above the fraud limit)', amount: '10000.01', error: null, shownAs: '€10,000.01' }
]

for (const { label, amount, error, shownAs } of amountTestdata) {
  test(`TC-SP-11: Amount ${label} should be ${error ? 'rejected' : 'accepted'}`, async ({ page }) => {

    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs('alice', 'password123');
    await new AccountsOverviewPage(page).gotoScheduledPayments();

    const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
    await scheduledPaymentsPage.reviewPayment('NL01VALU0000000003', 'Bob', amount, isoDate(1), `Amount ${runId}`);

    if (error) {
      await expect(scheduledPaymentsPage.formErrorLocator).toBeVisible();
      await expect(scheduledPaymentsPage.formErrorLocator).toHaveText(error);
      await expect(scheduledPaymentsPage.confirmationLocator).toHaveCount(0);
    } else {
      await expect(scheduledPaymentsPage.summaryValueLocatorFor('Amount')).toHaveText(shownAs!);
    }
  });
}

// Locks in current behaviour (the spec defines no precision): the database stores amounts
// with two decimals, rounding half up.
const decimalsTestdata = [
  { amount: '0.001', shownAs: '€0.00', stored: 0 },
  { amount: '10.005', shownAs: '€10.01', stored: 10.01 }
]

for (const { amount, shownAs, stored } of decimalsTestdata) {
  test(`TC-SP-12: Amount ${amount} should be stored and shown rounded to two decimals`, async ({ page, request }) => {

    const description = `Decimals ${runId}`;
    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs('alice', 'password123');
    await new AccountsOverviewPage(page).gotoScheduledPayments();

    const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
    await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', 'Bob', amount, isoDate(1), description);

    await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('SCHEDULED');
    await expect(scheduledPaymentsPage.entryLocatorFor(description)).toContainText(shownAs);

    const scheduledPayments = await new PaymentsApi(request).findScheduledPayments(1, description);
    expect(scheduledPayments[0].amount).toEqual(stored);
  });
}

const leapDay = nextLeapDay();
const dateTestdata = [
  { label: 'empty', date: '', error: DATE_ERROR },
  { label: 'yesterday', date: isoDate(-1), error: DATE_ERROR },
  { label: 'today', date: isoDate(0), error: DATE_ERROR },
  { label: 'tomorrow', date: isoDate(1), error: null },
  { label: 'in 365 days', date: isoDate(365), error: null },
  { label: `29 February (${leapDay})`, date: leapDay, error: null }
]

for (const { label, date, error } of dateTestdata) {
  test(`TC-SP-13: Execution date ${label} should be ${error ? 'rejected' : 'accepted'}`, async ({ page }) => {

    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs('alice', 'password123');
    await new AccountsOverviewPage(page).gotoScheduledPayments();

    const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
    await scheduledPaymentsPage.reviewPayment('NL01VALU0000000003', 'Bob', '10.00', date, `Date ${runId}`);

    if (error) {
      await expect(scheduledPaymentsPage.formErrorLocator).toBeVisible();
      await expect(scheduledPaymentsPage.formErrorLocator).toHaveText(error);
      await expect(scheduledPaymentsPage.confirmationLocator).toHaveCount(0);
    } else {
      await expect(scheduledPaymentsPage.summaryValueLocatorFor('Execution date')).toHaveText(displayDate(date));
    }
  });
}

test('TC-SP-13: Execution date 30 February should not be accepted by the date field', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);

  // The browser's date field refuses a non-existent date, so filling the form fails.
  await expect(scheduledPaymentsPage.reviewPayment('NL01VALU0000000003', 'Bob', '10.00', '2027-02-30', `Date ${runId}`))
    .rejects.toThrow('Malformed value');
});

// Locks in current behaviour: there is no maximum execution date.
test('TC-SP-14: A payment for 31 December 9999 should be scheduled', async ({ page }) => {

  const description = `Far future ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', 'Bob', '10.00', '9999-12-31', description);

  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('SCHEDULED');
  await expect(scheduledPaymentsPage.entryLocatorFor(description)).toContainText('Execution date: 12/31/9999');
});

const validationOrderTestdata = [
  { label: 'IBAN, amount and date invalid', iban: '', amount: '0', date: '', error: IBAN_ERROR },
  { label: 'amount and date invalid', iban: 'NL01VALU0000000003', amount: '0', date: '', error: AMOUNT_ERROR },
  { label: 'only the date invalid', iban: 'NL01VALU0000000003', amount: '10.00', date: '', error: DATE_ERROR }
]

for (const { label, iban, amount, date, error } of validationOrderTestdata) {
  test(`TC-SP-15: With ${label}, only the first failing field should be reported`, async ({ page }) => {

    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs('alice', 'password123');
    await new AccountsOverviewPage(page).gotoScheduledPayments();

    const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
    await scheduledPaymentsPage.reviewPayment(iban, 'Bob', amount, date, `Validation order ${runId}`);

    await expect(scheduledPaymentsPage.formErrorLocator).toBeVisible();
    await expect(scheduledPaymentsPage.formErrorLocator).toHaveText(error);
  });
}

// Locks in current behaviour: the beneficiary name is not validated.
test('TC-SP-16: A payment with an empty beneficiary name should be scheduled', async ({ page, request }) => {

  const description = `No name ${runId}`;
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', '', '10.00', isoDate(1), description);

  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('SCHEDULED');

  const scheduledPayments = await new PaymentsApi(request).findScheduledPayments(1, description);
  expect(scheduledPayments[0].toAccountName).toEqual('');
});

test('TC-SP-18: The error message should disappear as soon as the field changes', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.reviewPayment('', 'Bob', '10.00', isoDate(1), `Error reset ${runId}`);
  await expect(scheduledPaymentsPage.formErrorLocator).toBeVisible();

  await scheduledPaymentsPage.enterRecipientIban('NL01VALU0000000003');

  await expect(scheduledPaymentsPage.formErrorLocator).toHaveCount(0);
});

// ---------- D. Account ownership and access ----------

test('TC-SP-30: Only the customer\'s own accounts should be offered as source account', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('bob', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);

  await expect(scheduledPaymentsPage.fromAccountOptionsLocator).toHaveCount(2);
  await expect(scheduledPaymentsPage.fromAccountOptionsLocator).toContainText(['NL01VALU0000000003', 'NL01VALU0000000004']);
});

test('TC-SP-32: Scheduling from another customer\'s account should be rejected by the backend', async ({ request }) => {

  const description = `Not owned ${runId}`;
  const paymentsApi = new PaymentsApi(request);

  const rejectedResponse = await paymentsApi.schedulePayment(validRequest(1, 3, description));

  expect(rejectedResponse.status()).toEqual(403);
  expect(await rejectedResponse.json()).toEqual({ error: 'Source account does not belong to this customer' });
  expect((await paymentsApi.findScheduledPayments(1, description)).length).toEqual(0);
  expect((await paymentsApi.findScheduledPayments(2, description)).length).toEqual(0);

  const acceptedResponse = await paymentsApi.schedulePayment(validRequest(2, 3, description));

  expect(acceptedResponse.status()).toEqual(201);
});

test('TC-SP-33: Customers should only see their own scheduled payments', async ({ page }) => {

  const aliceDescription = `Alice only ${runId}`;
  const bobDescription = `Bob only ${runId}`;
  const loginPage = new LoginPage(page);
  const accountsOverviewPage = new AccountsOverviewPage(page);
  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);

  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await accountsOverviewPage.gotoScheduledPayments();
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', 'Bob', '10.00', isoDate(1), aliceDescription);
  await accountsOverviewPage.logout();

  await loginPage.loginAs('bob', 'password123');
  await accountsOverviewPage.gotoScheduledPayments();
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000001', 'Alice', '10.00', isoDate(1), bobDescription);
  await accountsOverviewPage.logout();

  await loginPage.loginAs('alice', 'password123');
  await accountsOverviewPage.gotoScheduledPayments();

  await expect(scheduledPaymentsPage.entryLocatorFor(aliceDescription)).toHaveCount(1);
  await expect(scheduledPaymentsPage.entryLocatorFor(bobDescription)).toHaveCount(0);
});

// Locks in current behaviour (known risk): the backend trusts the customerId it receives.
test('TC-SP-34: The backend should accept requests for any customer id without credentials', async ({ request }) => {

  const paymentsApi = new PaymentsApi(request);
  const existing = await paymentsApi.scheduleValidPayment({ description: `Trusted id ${runId}` });

  const listResponse = await request.get('http://localhost:8082/api/customers/1/scheduled-payments');

  expect(listResponse.status()).toEqual(200);
  expect((await paymentsApi.findScheduledPayments(1, `Trusted id ${runId}`)).length).toEqual(1);

  const scheduleResponse = await paymentsApi.schedulePayment(validRequest(1, 1, `Trusted id schedule ${runId}`));

  expect(scheduleResponse.status()).toEqual(201);

  const cancelResponse = await paymentsApi.cancelScheduledPayment(existing.id, 1);

  expect(cancelResponse.status()).toEqual(200);
  expect((await cancelResponse.json()).status).toEqual('CANCELLED');
});

test('TC-SP-35: Opening the scheduled payments page without logging in should redirect to the login page', async ({ page }) => {

  await new ScheduledPaymentsPage(page).open();

  await expect(page).toHaveURL(/\/login$/);
});

test('TC-SP-36: The admin should not see a Scheduled payments link', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('admin', 'admin123');

  await expect(page).toHaveURL(/\/admin$/);
  await expect(new AdminPage(page).scheduledPaymentsLinkLocator).toHaveCount(0);
});

// Locks in current behaviour: the route is not admin-only, and the admin has no accounts.
test('TC-SP-37: The admin opening the scheduled payments page directly should get a form without source accounts', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('admin', 'admin123');
  await expect(page).toHaveURL(/\/admin$/);

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.open();

  await expect(scheduledPaymentsPage.emptyListMessageLocator).toBeVisible();
  await expect(scheduledPaymentsPage.reviewButtonLocator).toBeDisabled();
  await expect(scheduledPaymentsPage.fromAccountOptionsLocator).toHaveCount(0);
});

// ---------- E. Scheduled payments list ----------

test('TC-SP-41: Scheduled payments should be listed by execution date, then by creation', async ({ page, request }) => {

  const paymentsApi = new PaymentsApi(request);
  await paymentsApi.scheduleValidPayment({ amount: 3.00, executionDate: isoDate(3), description: `Order 3.00 ${runId}` });
  await paymentsApi.scheduleValidPayment({ amount: 1.00, executionDate: isoDate(1), description: `Order 1.00 ${runId}` });
  await paymentsApi.scheduleValidPayment({ amount: 2.00, executionDate: isoDate(2), description: `Order 2.00 ${runId}` });
  await paymentsApi.scheduleValidPayment({ amount: 4.00, executionDate: isoDate(1), description: `Order 4.00 ${runId}` });

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.reload();

  const descriptions = (await scheduledPaymentsPage.getDescriptionsInListOrder()).filter((text) => text.includes(runId));
  expect(descriptions).toEqual([
    `Order 1.00 ${runId}`,
    `Order 4.00 ${runId}`,
    `Order 2.00 ${runId}`,
    `Order 3.00 ${runId}`
  ]);
});

test('TC-SP-43: The list should still be shown when the accounts can\'t be loaded', async ({ page, request }) => {

  const description = `Accounts down ${runId}`;
  await new PaymentsApi(request).scheduleValidPayment({ description });

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  // From here on the page can't reach the accounts endpoint, as if the Accounts Service were down.
  await failAccountsRequests(page);
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);

  await expect(scheduledPaymentsPage.errorMessageLocator).toBeVisible();
  await expect(scheduledPaymentsPage.reviewButtonLocator).toBeDisabled();
  await expect(scheduledPaymentsPage.entryLocatorFor(description)).toContainText('From account 1');
});

// ---------- F. Cancelling ----------

test('TC-SP-53: Cancelling another customer\'s or a non-existent payment should be rejected', async ({ request }) => {

  const description = `Cancel foreign ${runId}`;
  const paymentsApi = new PaymentsApi(request);
  const scheduledPayment = await paymentsApi.scheduleValidPayment({ description });

  const foreignResponse = await paymentsApi.cancelScheduledPayment(scheduledPayment.id, 2);

  expect(foreignResponse.status()).toEqual(404);
  expect(await foreignResponse.json()).toEqual({ error: `Scheduled payment ${scheduledPayment.id} not found` });
  expect((await paymentsApi.findScheduledPayments(1, description))[0].status).toEqual('SCHEDULED');

  const unknownResponse = await paymentsApi.cancelScheduledPayment(999999, 1);

  expect(unknownResponse.status()).toEqual(404);
  expect(await unknownResponse.json()).toEqual({ error: 'Scheduled payment 999999 not found' });

  const noCustomerResponse = await paymentsApi.cancelScheduledPayment(scheduledPayment.id);

  // Locks in current behaviour: Spring's default error body, not ValuBank's {"error": "..."} message.
  expect(noCustomerResponse.status()).toEqual(400);
  expect((await noCustomerResponse.json()).error).toEqual('Bad Request');
});

// ---------- I. Executed exactly once ----------

test('TC-SP-81: Executing a non-existent scheduled payment should be rejected', async ({ request }) => {

  const response = await new PaymentsApi(request).executeScheduledPayment(999999);

  expect(response.status()).toEqual(404);
  expect(await response.json()).toEqual({ error: 'Scheduled payment 999999 not found' });
});

/** A valid schedule request for the given customer and source account. */
function validRequest(customerId: number, fromAccountId: number, description: string) {
  return {
    customerId,
    fromAccountId,
    toAccountIban: 'NL01VALU0000000003',
    toAccountName: 'Bob de Vries',
    amount: 10.00,
    executionDate: isoDate(1),
    description
  };
}
