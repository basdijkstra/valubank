import { Page } from "@playwright/test";

export class AccountsOverviewPage {

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async getAccountBalance(iban: string): Promise<string> {
        const balanceLocator = this.page.locator(`xpath=//div[@class='account-card-iban' and text()='${iban}']/following-sibling::div[@class='account-card-balance']`);
        await balanceLocator.waitFor({ state: 'visible' });
        return await balanceLocator.innerText();
    }

    async logout() {
        await this.page.getByRole('button', { name: 'Log out' }).click();
    }
}