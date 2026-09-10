import test, { expect } from "@playwright/test";
import { LoginPage } from "./pages/loginPage";
import { AccountsOverviewPage } from './pages/accountsOverviewPage';
import { AccountDetailsPage } from "./pages/accountDetailsPage";

test('The currency dropdown value defaults to the source account currency when making a payment', async ({ page }) => {
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

  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');
  
  await expect(new AccountDetailsPage(page).dropdownCurrency).toHaveValue('GBP');
});

test('The currency dropdown value defaults to EUR when API returns an unsupported value', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.open();
  await loginPage.loginAs('alice', 'password123');

  /**
   * TODO: create a mock similar to the previous test,
   * but now return an unsupported currency value (e.g. "CAD")
   * and assert that the dropdown defaults to "EUR"
   */

  await new AccountsOverviewPage(page).gotoAccountDetails('NL01VALU0000000001');
  
  await expect(new AccountDetailsPage(page).dropdownCurrency).toHaveValue('EUR');
});
