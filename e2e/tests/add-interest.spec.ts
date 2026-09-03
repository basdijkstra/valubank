import { test, expect } from '@playwright/test';
import currency from 'currency.js';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AdminPage } from './pages/adminPage';

test('Interest is properly adding to savings account for Alice', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  const accountsOverviewPage = new AccountsOverviewPage(page);

  const initialBalance = await accountsOverviewPage.getAccountBalance('NL01VALU0000000002');

  console.log(`Initial balance for Alice's savings account: ${initialBalance}`);

  await accountsOverviewPage.logout();

  await loginPage.loginAs('admin', 'admin123');

  const adminPage = new AdminPage(page);
  await adminPage.addInterestToAccount('NL01VALU0000000002');
  await adminPage.logout();

  await loginPage.loginAs('alice', 'password123');
  
  const updatedBalance = await accountsOverviewPage.getAccountBalance('NL01VALU0000000002');

  console.log(`Updated balance for Alice's savings account: ${updatedBalance}`);

  expect(currency(updatedBalance)).toEqual(currency(initialBalance).multiply(1.015));
});
