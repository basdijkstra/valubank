import { test, expect } from '@playwright/test';
import currency from 'currency.js';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from './pages/accountDetailsPage';

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

test('Paying in the same currency as the source account does not convert the amount before debiting', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  const accountsOverviewPage = new AccountsOverviewPage(page);
  const initialBalance = await accountsOverviewPage.getAccountBalance('NL01VALU0000000001');

  await accountsOverviewPage.gotoAccountDetails('NL01VALU0000000001');
  const accountDetailsPage = new AccountDetailsPage(page);
  await accountDetailsPage.makePayment(
    'NL01VALU0000000003', 'Bob', '100.00', ' Same currency test payment', 'EUR'
  );

  await expect(page.locator('xpath=//div[contains(@class, "banner-success")]')).toHaveText('COMPLETED');

  const updatedBalance = await accountDetailsPage.getBalance();

  const expectedBalance = currency(initialBalance).subtract(currency(100));
  expect(currency(updatedBalance)).toEqual(expectedBalance);
});
