import { test, expect } from '@playwright/test';
import currency from 'currency.js';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from './pages/accountDetailsPage';

// Alice's checking account is EUR-denominated. Paying in USD forces the
// Accounts Service to convert via the Currency Rate Service before debiting.
// The seeded USD -> EUR rate is fixed at 0.93, so this is the only way to
// prove the conversion actually happened correctly end to end: the balance
// mutation and the exchange rate lookup both happen inside the Accounts
// Service, with nothing about the conversion visible from the payment
// response itself (it only reports status/reason, not the converted amount).
const usdToEurRate = 0.93;

test('Paying in a different currency than the source account converts the amount before debiting', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  const accountsOverviewPage = new AccountsOverviewPage(page);
  const initialBalance = await accountsOverviewPage.getAccountBalance('NL01VALU0000000001');

  await accountsOverviewPage.gotoAccountDetails('NL01VALU0000000001');

  const accountDetailsPage = new AccountDetailsPage(page);
  await accountDetailsPage.makePayment(
    'NL01VALU0000000003', 'Bob', '100.00', 'Cross-currency test payment', 'USD'
  );

  await expect(page.locator('xpath=//div[contains(@class, "banner-success")]')).toHaveText('COMPLETED');

  const updatedBalance = await accountDetailsPage.getBalance();

  const expectedBalance = currency(initialBalance).subtract(currency(100).multiply(usdToEurRate));
  expect(currency(updatedBalance)).toEqual(expectedBalance);
});
