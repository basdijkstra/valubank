import test, { expect } from "@playwright/test";
import { LoginPage } from "./pages/loginPage";
import { AccountsOverviewPage } from "./pages/accountsOverviewPage";

const testdata = [
  { username: 'alice', password: 'password123', displayName: 'Alice Janssen' },
  { username: 'bob', password: 'password123', displayName: 'Bob de Vries' },
  { username: 'admin', password: 'admin123', displayName: 'ValuBank Admin' }
]

for (const { username, password, displayName } of testdata) {
  test(`User ${username} can login with valid credentials`, async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs(username, password);

    await expect(new AccountsOverviewPage(page).textLabelGreeting).toHaveText(`Hi, ${displayName}`);
  });
}
