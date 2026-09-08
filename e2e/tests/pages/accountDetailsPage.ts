import { Page } from "@playwright/test";

export class AccountDetailsPage {

    private readonly page: Page;
    readonly errorMessageLocator;

    constructor(page: Page) {
        this.page = page;
        this.errorMessageLocator = this.page.locator('xpath=//div[contains(@class, "banner-error")]');
    }

    async getBalance(): Promise<string> {
        const balanceLocator = this.page.locator('.account-summary-balance');
        await balanceLocator.waitFor({ state: 'visible' });
        return await balanceLocator.innerText();
    }

    async makePayment(recipientIban: string, recipientName: string, amount: string, description: string, currency?: string) {
        await this.page.getByLabel('Destination IBAN').fill(recipientIban);
        await this.page.getByLabel('Destination name').fill(recipientName);
        await this.page.getByLabel('Amount').fill(amount);
        await this.page.getByLabel('Description').fill(description);
        if (currency) {
            await this.page.getByLabel('Currency').selectOption(currency);
        }
        await this.page.getByRole('button', { name: 'Send payment' }).click();
    }
}