import { test, expect } from '@playwright/test';
import currency from 'currency.js';
import { LoginPage } from './pages/loginPage';
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AdminPage } from './pages/adminPage';

const testdata = [
  { username: 'alice', password: 'password123', iban: 'NL01VALU0000000001', type: 'checking', interestRate: 0.001 },
  { username: 'alice', password: 'password123', iban: 'NL01VALU0000000002', type: 'savings', interestRate: 0.015 },
  { username: 'bob', password: 'password123', iban: 'NL01VALU0000000003', type: 'checking', interestRate: 0.001 }
]

for (const { username, password, iban, type, interestRate } of testdata) {
  test(`Interest is properly adding ${interestRate} interest to ${username}'s ${type} account`, async ({ page }) => {

    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs(username, password);

    const accountsOverviewPage = new AccountsOverviewPage(page);

    const initialBalance = await accountsOverviewPage.getAccountBalance(iban);

    console.log(`Initial balance for ${username}'s ${type} account: ${initialBalance}`);

    await accountsOverviewPage.logout();

    await loginPage.loginAs('admin', 'admin123');

    const adminPage = new AdminPage(page);
    await adminPage.addInterestToAccount(iban);
    await adminPage.logout();

    await loginPage.loginAs(username, password);
    
    const updatedBalance = await accountsOverviewPage.getAccountBalance(iban);

    console.log(`Updated balance for ${username}'s ${type} account: ${updatedBalance}`);

    expect(currency(updatedBalance)).toEqual(currency(initialBalance).multiply(1 + interestRate));
  });
}
