import { Page } from "@playwright/test";

export class AdminPage {

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }
    
    async addInterestToAccount(iban: string) {
        await this.page.locator(`xpath=//td[@class='account-card-iban' and text()='${iban}']/preceding-sibling::td/input[@type='checkbox']`).check();
        await this.page.getByRole('button', { name: `Add interest to selected (1)` }).click();
        await this.page.locator("xpath=//div[contains(@class,'banner-success')]").waitFor({ state: 'visible' });
    }

    async logout() {
        await this.page.getByRole('button', { name: 'Log out' }).click();
    }
}