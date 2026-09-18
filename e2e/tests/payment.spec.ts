import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from './pages/accountDetailsPage';

test('Trying to make a payment going over the 5000 overdraft limit should fail with an error message', async ({ page }) => {

  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');

  const accountDetailsPage = new AccountDetailsPage(page);
  await accountDetailsPage.makePayment('NL01VALU0000000099', 'Bob', '8000.00', 'Too many bananas');

  await expect(accountDetailsPage.errorMessageLocator).toBeVisible();
  await expect(accountDetailsPage.errorMessageLocator).toHaveText('REJECTED - Insufficient funds');
});
