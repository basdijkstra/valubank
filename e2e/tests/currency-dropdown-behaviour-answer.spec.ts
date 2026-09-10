import test, { expect } from "@playwright/test";
import { LoginPage } from "./pages/loginPage";
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from "./pages/accountDetailsPage";

test.skip('The currency dropdown value defaults to the source account currency when making a payment', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  /**
   * TODO: Have Playwright intercept the request to the backend that fetches the account details
   *   and mock the response data using these values:
   * - id: 1
   * - customerId: 1
   * - iban: "NL01VALU0000000001"
   * - accountType: "CHECKING"
   * - balance: 2309.50
   * - currency: "GBP"
   */
  await page.route('**/api/accounts/1', async (route) => {
    const jsonResponse = {
      id: 1,
      customerId: 1,
      iban: "NL01VALU0000000001",
      accountType: "CHECKING",
      balance: 2309.50,
      currency: "GBP"
    };
    await route.fulfill({ status: 200, json: jsonResponse });
  });

  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');
  
  await expect(new AccountDetailsPage(page).dropdownCurrency).toHaveValue('GBP');
});

test.skip('The currency dropdown value defaults to EUR when API returns an unsupported value', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  /**
   * TODO: similar to the previous test, but now return an unsupported currency value (e.g. "CAD")
   * and assert that the dropdown defaults to "EUR"
   */
  await page.route('**/api/accounts/1', async (route) => {
    const jsonResponse = {
      id: 1,
      customerId: 1,
      iban: "NL01VALU0000000001",
      accountType: "CHECKING",
      balance: 2309.50,
      currency: "CAD" // Unsupported currency
    };
    await route.fulfill({ status: 200, json: jsonResponse });
  });

  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');
  
  await expect(new AccountDetailsPage(page).dropdownCurrency).toHaveValue('EUR');
});