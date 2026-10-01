import { test, expect, APIRequestContext } from '@playwright/test';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { ScheduledPaymentsPage } from './pages/scheduledPaymentsPage';

const PAYMENTS_API_URL = 'http://localhost:8082';

function tomorrow(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

// Test-only hook on the Payments Service: execute a scheduled payment now instead of on its date.
async function executeNow(request: APIRequestContext, scheduledPaymentId: string) {
  return request.post(`${PAYMENTS_API_URL}/api/test-support/scheduled-payments/${scheduledPaymentId}/execute`);
}

test.beforeEach(async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');
});

test('A scheduled payment is executed once and then shows up in payment history', async ({ page, request }) => {

  const description = `Scheduled rent ${Date.now()}`;
  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.open();
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000001', 'NL01VALU0000000003', 'Bob', '12.34', tomorrow(), description);

  await expect(scheduledPaymentsPage.successMessageLocator).toContainText('SCHEDULED');
  await expect(scheduledPaymentsPage.scheduledPayment(description)).toContainText('SCHEDULED');

  const id = await scheduledPaymentsPage.scheduledPaymentId(description);
  const firstExecution = await executeNow(request, id);
  expect(firstExecution.status()).toBe(200);
  expect((await firstExecution.json()).status).toBe('EXECUTED');

  // Executed once: a second attempt is refused.
  expect((await executeNow(request, id)).status()).toBe(409);

  await page.reload();
  await expect(scheduledPaymentsPage.scheduledPayment(description)).toContainText('EXECUTED');
  await expect(scheduledPaymentsPage.scheduledPayment(description).getByRole('button', { name: 'Cancel' })).toHaveCount(0);

  await page.getByRole('link', { name: /Back to accounts/ }).click();
  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');
  const historyItem = page.locator('.payment-list-item').filter({ hasText: description });
  await expect(historyItem).toHaveCount(1);
  await expect(historyItem).toContainText('COMPLETED');
});

test('A scheduled payment can be cancelled before execution, and is then never executed', async ({ page, request }) => {

  const description = `Cancelled payment ${Date.now()}`;
  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.open();
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000001', 'NL01VALU0000000003', 'Bob', '5.00', tomorrow(), description);
  const id = await scheduledPaymentsPage.scheduledPaymentId(description);

  await scheduledPaymentsPage.cancel(description);

  await expect(scheduledPaymentsPage.scheduledPayment(description)).toContainText('CANCELLED');
  expect((await executeNow(request, id)).status()).toBe(409);
});

test('A scheduled payment that is rejected on execution gets status FAILED with a reason', async ({ page, request }) => {

  const description = `Blocked payment ${Date.now()}`;
  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.open();
  await scheduledPaymentsPage.schedulePayment('NL01VALU0000000001', 'NL99BLOCKED0000000', 'Mallory', '5.00', tomorrow(), description);
  const id = await scheduledPaymentsPage.scheduledPaymentId(description);

  await executeNow(request, id);
  await page.reload();

  await expect(scheduledPaymentsPage.scheduledPayment(description)).toContainText('FAILED');
  await expect(scheduledPaymentsPage.scheduledPayment(description)).toContainText('Destination account is flagged for fraud');
});

test('An execution date that is not in the future is refused', async ({ page }) => {

  const today = new Date();
  const todayIso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  const scheduledPaymentsPage = new ScheduledPaymentsPage(page);
  await scheduledPaymentsPage.open();

  await page.getByLabel('Beneficiary IBAN').fill('NL01VALU0000000003');
  await page.getByLabel('Beneficiary name').fill('Bob');
  await page.getByLabel(/^Amount/).fill('5.00');
  await page.getByLabel('Execution date').fill(todayIso);
  await page.getByRole('button', { name: 'Review payment' }).click();

  await expect(scheduledPaymentsPage.errorMessageLocator).toHaveText('Execution date must be in the future.');
  await expect(page.getByRole('button', { name: 'Confirm' })).toHaveCount(0);
});
