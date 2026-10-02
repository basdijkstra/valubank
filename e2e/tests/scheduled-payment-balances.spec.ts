import { test, expect } from '@playwright/test';
import currency from 'currency.js';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from './pages/accountDetailsPage';
import { ScheduledPaymentsPage } from './pages/scheduledPaymentsPage';
import { AccountsApi } from './helpers/accountsApi';
import { PaymentsApi } from './helpers/paymentsApi';
import { isoDate, displayDate } from './helpers/dates';
import { uniqueId, cleanUpScheduledPayments } from './helpers/testData';

// Test cases: docs/test-cases/scheduled-payments.md. These tests execute payments and check
// balances, so they run one at a time in the 'scheduled-payments-balances' project, after
// all other tests (see playwright.config.ts).

// displayDate() and the expected amount formats assume the en-US locale.
test.use({ locale: 'en-US' });

let runId: string;
let originalBalances: Map<number, string>;

test.beforeEach(async ({ request }) => {
  runId = uniqueId();
  const accountsApi = new AccountsApi(request);
  originalBalances = new Map();
  for (const accountId of [1, 2, 3]) {
    originalBalances.set(accountId, await accountsApi.getBalance(accountId));
  }
});

test.afterEach(async ({ request }) => {
  // Cancel what is still scheduled first, then put every balance back to what it was before the test.
  await cleanUpScheduledPayments(request, runId);
  const accountsApi = new AccountsApi(request);
  for (const [accountId, balance] of originalBalances) {
    await accountsApi.setBalance(accountId, balance);
  }
});

// ---------- A. Scheduling via the UI: main flow ----------

test('TC-SP-01: Scheduling a valid payment for tomorrow should list it as scheduled without debiting the source account', async ({ page, request }) => {

  const description = `Concert tickets ${runId}`;
  const executionDate = isoDate(1);
  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  const initialBalance = await accountsApi.getBalance(1);

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.reviewPayment('NL01VALU0000000003', 'Bob de Vries', '25.50', executionDate, description);
  await scheduledPaymentsPage.confirmPayment();

  await expect(scheduledPaymentsPage.successMessageLocator).toBeVisible();
  await expect(scheduledPaymentsPage.successMessageLocator).toHaveText(`SCHEDULED - payment scheduled for ${displayDate(executionDate)}`);
  // The form is reset, but keeps the selected account (id 1 is NL01VALU0000000001).
  await expect(scheduledPaymentsPage.fromAccountLocator).toHaveValue('1');
  await expect(scheduledPaymentsPage.recipientIbanLocator).toHaveValue('');
  await expect(scheduledPaymentsPage.recipientNameLocator).toHaveValue('');
  await expect(scheduledPaymentsPage.amountLocator).toHaveValue('');
  await expect(scheduledPaymentsPage.executionDateLocator).toHaveValue('');
  await expect(scheduledPaymentsPage.descriptionLocator).toHaveValue('');

  const entry = scheduledPaymentsPage.entryLocatorFor(description);
  await expect(entry).toHaveCount(1);
  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('SCHEDULED');
  await expect(entry).toContainText('Bob de Vries');
  await expect(entry).toContainText('NL01VALU0000000003');
  await expect(entry).toContainText('€25.50');
  await expect(entry).toContainText('From NL01VALU0000000001');
  await expect(entry).toContainText(`Execution date: ${displayDate(executionDate)}`);

  // Scheduling neither debits the account nor creates a payment in its history.
  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance));
  const payments = (await paymentsApi.getPaymentsForAccount(1)).filter((payment) => payment.description === description);
  expect(payments.length).toEqual(0);

  const scheduledPayments = await paymentsApi.findScheduledPayments(1, description);
  expect(scheduledPayments.length).toEqual(1);
  expect(scheduledPayments[0]).toEqual(expect.objectContaining({
    status: 'SCHEDULED',
    amount: 25.5,
    currency: 'EUR',
    executionDate,
    customerId: 1,
    fromAccountId: 1,
    paymentId: null,
    reason: null,
    executedAt: null,
    cancelledAt: null
  }));
});

// ---------- E. Scheduled payments list ----------

// Locks in current behaviour: past and future payments are shown in one list.
test('TC-SP-42: Scheduled payments in every status should be listed, with Cancel and reason only where they apply', async ({ page, request }) => {

  const paymentsApi = new PaymentsApi(request);
  await paymentsApi.scheduleValidPayment({ description: `Status scheduled ${runId}` });
  const cancelled = await paymentsApi.scheduleValidPayment({ description: `Status cancelled ${runId}` });
  await paymentsApi.cancelScheduledPayment(cancelled.id, 1);
  const executed = await paymentsApi.scheduleValidPayment({ amount: 1.00, description: `Status executed ${runId}` });
  await paymentsApi.executeScheduledPayment(executed.id);
  // Above the fraud limit, so the execution fails.
  const failed = await paymentsApi.scheduleValidPayment({ fromAccountId: 2, amount: 10000.01, description: `Status failed ${runId}` });
  await paymentsApi.executeScheduledPayment(failed.id);

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  const expectations = [
    { status: 'SCHEDULED', cancellable: 1, reasons: 0 },
    { status: 'CANCELLED', cancellable: 0, reasons: 0 },
    { status: 'EXECUTED', cancellable: 0, reasons: 0 },
    { status: 'FAILED', cancellable: 0, reasons: 1 }
  ];
  for (const { status, cancellable, reasons } of expectations) {
    const description = `Status ${status.toLowerCase()} ${runId}`;
    await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText(status);
    await expect(scheduledPaymentsPage.cancelButtonLocatorFor(description)).toHaveCount(cancellable);
    await expect(scheduledPaymentsPage.reasonLocatorFor(description)).toHaveCount(reasons);
  }
});

// ---------- F. Cancelling ----------

test('TC-SP-50: Cancelling a scheduled payment should mark it cancelled without debiting the account', async ({ page, request }) => {

  const description = `Cancel me ${runId}`;
  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  await paymentsApi.scheduleValidPayment({ amount: 7.00, executionDate: isoDate(2), description });
  const initialBalance = await accountsApi.getBalance(1);

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.cancelScheduledPayment(description);

  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('CANCELLED');
  await expect(scheduledPaymentsPage.cancelButtonLocatorFor(description)).toHaveCount(0);

  const [scheduledPayment] = await paymentsApi.findScheduledPayments(1, description);
  expect(scheduledPayment.status).toEqual('CANCELLED');
  expect(scheduledPayment.cancelledAt).not.toEqual(null);
  expect(scheduledPayment.executedAt).toEqual(null);
  expect(scheduledPayment.paymentId).toEqual(null);

  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance));
  const payments = (await paymentsApi.getPaymentsForAccount(1)).filter((payment) => payment.description === description);
  expect(payments.length).toEqual(0);
});

test('TC-SP-51: A cancelled payment should never be executed', async ({ request }) => {

  const description = `Cancelled execute ${runId}`;
  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  const scheduledPayment = await paymentsApi.scheduleValidPayment({ amount: 7.00, description });
  await paymentsApi.cancelScheduledPayment(scheduledPayment.id, 1);
  const initialBalance = await accountsApi.getBalance(1);

  const response = await paymentsApi.executeScheduledPayment(scheduledPayment.id);

  expect(response.status()).toEqual(409);
  expect(await response.json()).toEqual({ error: 'Scheduled payment is CANCELLED and can no longer be executed' });
  expect((await paymentsApi.findScheduledPayments(1, description))[0].status).toEqual('CANCELLED');
  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance));
  const payments = (await paymentsApi.getPaymentsForAccount(1)).filter((payment) => payment.description === description);
  expect(payments.length).toEqual(0);
});

test('TC-SP-54: An executed payment should not be cancellable', async ({ page, request }) => {

  const description = `Executed cancel ${runId}`;
  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  const scheduledPayment = await paymentsApi.scheduleValidPayment({ amount: 1.00, description });
  await paymentsApi.executeScheduledPayment(scheduledPayment.id);
  const balanceAfterExecution = await accountsApi.getBalance(1);

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('EXECUTED');
  await expect(scheduledPaymentsPage.cancelButtonLocatorFor(description)).toHaveCount(0);

  const response = await paymentsApi.cancelScheduledPayment(scheduledPayment.id, 1);

  expect(response.status()).toEqual(409);
  expect(await response.json()).toEqual({ error: 'Scheduled payment is EXECUTED and can no longer be cancelled' });
  expect((await paymentsApi.findScheduledPayments(1, description))[0].status).toEqual('EXECUTED');
  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(balanceAfterExecution));
  const payments = (await paymentsApi.getPaymentsForAccount(1)).filter((payment) => payment.description === description);
  expect(payments.length).toEqual(1);
});

test('TC-SP-56: Cancelling from a stale page after execution should show an error and debit only once', async ({ page, request }) => {

  const description = `Stale cancel ${runId}`;
  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  const initialBalance = await accountsApi.getBalance(1);

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoScheduledPayments();

  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000003', 'Bob', '4.00', isoDate(1), description);

  // Executed behind the page's back: the page still shows it as SCHEDULED.
  const [scheduledPayment] = await paymentsApi.findScheduledPayments(1, description);
  const executeResponse = await paymentsApi.executeScheduledPayment(scheduledPayment.id);

  expect(executeResponse.status()).toEqual(200);
  expect((await executeResponse.json()).status).toEqual('EXECUTED');

  await scheduledPaymentsPage.cancelScheduledPayment(description);

  await expect(scheduledPaymentsPage.errorMessageLocator).toBeVisible();
  await expect(scheduledPaymentsPage.errorMessageLocator).toHaveText('Scheduled payment is EXECUTED and can no longer be cancelled');
  await expect(scheduledPaymentsPage.statusLocatorFor(description)).toHaveText('EXECUTED');
  await expect(scheduledPaymentsPage.cancelButtonLocatorFor(description)).toHaveCount(0);
  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance).subtract('4.00'));
});

// ---------- H. Balance and fraud boundaries at execution ----------
// These tests set the seeded starting balance themselves, so they assert absolute balances.

test('TC-SP-70: A payment just above the fraud limit should fail and one at the limit should be executed', async ({ request }) => {

  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  await accountsApi.setBalance(2, '11000.00');

  const aboveLimit = await scheduleAndExecute(paymentsApi, 1, 2, '10000.01', `Fraud above ${runId}`);

  expect(aboveLimit.status).toEqual('FAILED');
  expect(aboveLimit.reason).toEqual('Amount exceeds maximum allowed per transaction (10000)');
  expect(currency(await accountsApi.getBalance(2))).toEqual(currency('11000.00'));
  const [rejectedPayment] = (await paymentsApi.getPaymentsForAccount(2)).filter((payment) => payment.description === `Fraud above ${runId}`);
  expect(rejectedPayment).toEqual(expect.objectContaining({
    status: 'REJECTED',
    amount: 10000.01,
    reason: 'Amount exceeds maximum allowed per transaction (10000)'
  }));

  const atLimit = await scheduleAndExecute(paymentsApi, 1, 2, '10000.00', `Fraud at limit ${runId}`);

  expect(atLimit.status).toEqual('EXECUTED');
  expect(currency(await accountsApi.getBalance(2))).toEqual(currency('1000.00'));
  const [completedPayment] = (await paymentsApi.getPaymentsForAccount(2)).filter((payment) => payment.description === `Fraud at limit ${runId}`);
  expect(completedPayment).toEqual(expect.objectContaining({ status: 'COMPLETED', amount: 10000 }));
});

test('TC-SP-71: A savings account should not go below zero', async ({ request }) => {

  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  await accountsApi.setBalance(2, '11000.00');

  const steps = [
    { amount: '10000.00', status: 'EXECUTED', balance: '1000.00' },
    { amount: '1000.01', status: 'FAILED', balance: '1000.00' },
    { amount: '1000.00', status: 'EXECUTED', balance: '0.00' },
    { amount: '0.01', status: 'FAILED', balance: '0.00' }
  ];
  for (const { amount, status, balance } of steps) {
    const result = await scheduleAndExecute(paymentsApi, 1, 2, amount, `Savings ${amount} ${runId}`);

    expect(result.status).toEqual(status);
    if (status === 'FAILED') {
      expect(result.reason).toEqual('Insufficient funds');
    }
    expect(currency(await accountsApi.getBalance(2))).toEqual(currency(balance));
  }
});

test('TC-SP-72: A checking account should not go below the overdraft limit of -5000', async ({ page, request }) => {

  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  await accountsApi.setBalance(1, '2500.00');

  const steps = [
    { amount: '7500.01', status: 'FAILED', balance: '2500.00' },
    { amount: '7500.00', status: 'EXECUTED', balance: '-5000.00' },
    { amount: '0.01', status: 'FAILED', balance: '-5000.00' }
  ];
  for (const { amount, status, balance } of steps) {
    const result = await scheduleAndExecute(paymentsApi, 1, 1, amount, `Checking ${amount} ${runId}`);

    expect(result.status).toEqual(status);
    if (status === 'FAILED') {
      expect(result.reason).toEqual('Insufficient funds');
    }
    expect(currency(await accountsApi.getBalance(1))).toEqual(currency(balance));
  }

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');

  await expect(new AccountDetailsPage(page).balanceLocator).toHaveText('-€5,000.00');
});

// Locks in current behaviour: same-day payments are executed in creation order.
test('TC-SP-73: Of two payments that together exceed the available funds, the first created should be executed', async ({ request }) => {

  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  await accountsApi.setBalance(3, '500.00');
  const paymentA = await paymentsApi.scheduleValidPayment({
    customerId: 2, fromAccountId: 3, toAccountIban: 'NL01VALU0000000001', toAccountName: 'Alice', amount: 3000.00, description: `Compete A ${runId}`
  });
  const paymentB = await paymentsApi.scheduleValidPayment({
    customerId: 2, fromAccountId: 3, toAccountIban: 'NL01VALU0000000001', toAccountName: 'Alice', amount: 3000.00, description: `Compete B ${runId}`
  });

  const resultA = await (await paymentsApi.executeScheduledPayment(paymentA.id)).json();

  expect(resultA.status).toEqual('EXECUTED');
  expect(currency(await accountsApi.getBalance(3))).toEqual(currency('-2500.00'));

  const resultB = await (await paymentsApi.executeScheduledPayment(paymentB.id)).json();

  expect(resultB.status).toEqual('FAILED');
  expect(resultB.reason).toEqual('Insufficient funds');
  expect(currency(await accountsApi.getBalance(3))).toEqual(currency('-2500.00'));
});

// ---------- I. Executed exactly once ----------

test('TC-SP-80: Executing a payment a second time should be rejected and not debit again', async ({ request }) => {

  const description = `Execute twice ${runId}`;
  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);
  const initialBalance = await accountsApi.getBalance(1);

  const firstResult = await scheduleAndExecute(paymentsApi, 1, 1, '9.00', description);

  expect(firstResult.status).toEqual('EXECUTED');
  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance).subtract('9.00'));

  const secondResponse = await paymentsApi.executeScheduledPayment(firstResult.id);

  expect(secondResponse.status()).toEqual(409);
  expect(await secondResponse.json()).toEqual({ error: 'Scheduled payment is EXECUTED and can no longer be executed' });
  expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance).subtract('9.00'));
  const payments = (await paymentsApi.getPaymentsForAccount(1)).filter((payment) => payment.description === description);
  expect(payments.length).toEqual(1);
});

test('TC-SP-82: Executing and cancelling a payment at the same time should do exactly one of both', async ({ request }) => {

  const accountsApi = new AccountsApi(request);
  const paymentsApi = new PaymentsApi(request);

  for (let attempt = 1; attempt <= 3; attempt++) {
    const description = `Race ${attempt} ${runId}`;
    const scheduledPayment = await paymentsApi.scheduleValidPayment({ amount: 8.00, description });
    const initialBalance = await accountsApi.getBalance(1);

    const responses = await Promise.all([
      paymentsApi.executeScheduledPayment(scheduledPayment.id),
      paymentsApi.cancelScheduledPayment(scheduledPayment.id, 1)
    ]);

    expect(responses.map((response) => response.status()).sort()).toEqual([200, 409]);

    const [finalState] = await paymentsApi.findScheduledPayments(1, description);
    const payments = (await paymentsApi.getPaymentsForAccount(1)).filter((payment) => payment.description === description);
    if (finalState.status === 'EXECUTED') {
      expect(payments.length).toEqual(1);
      expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance).subtract('8.00'));
    } else {
      expect(finalState.status).toEqual('CANCELLED');
      expect(payments.length).toEqual(0);
      expect(currency(await accountsApi.getBalance(1))).toEqual(currency(initialBalance));
    }
  }
});

/** Schedules a payment through the API and executes it right away. Returns the executed scheduled payment. */
async function scheduleAndExecute(paymentsApi: PaymentsApi, customerId: number, fromAccountId: number, amount: string, description: string) {
  const scheduledPayment = await paymentsApi.scheduleValidPayment({ customerId, fromAccountId, amount, description });
  const response = await paymentsApi.executeScheduledPayment(scheduledPayment.id);
  return await response.json();
}
