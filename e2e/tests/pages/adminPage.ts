import { Locator, Page } from "@playwright/test";

export class AdminPage {

    private readonly page: Page;
    readonly scheduledPaymentsLinkLocator: Locator;

    constructor(page: Page) {
        this.page = page;
        this.scheduledPaymentsLinkLocator = this.page.getByRole('link', { name: 'Scheduled payments' });
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