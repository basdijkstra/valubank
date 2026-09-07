import { Page } from "@playwright/test";

export class AccountDetailsPage {

    private readonly page: Page;
    readonly errorMessageLocator;

    constructor(page: Page) {
        this.page = page;
        this.errorMessageLocator = this.page.locator('xpath=//div[contains(@class, "banner-error")]');
    }

    async makePayment(recipientIban: string, recipientName: string, amount: string, description: string) {
        await this.page.getByLabel('Destination IBAN').fill(recipientIban);
        await this.page.getByLabel('Destination name').fill(recipientName);
        await this.page.getByLabel('Amount').fill(amount);
        await this.page.getByLabel('Description').fill(description);
        await this.page.getByRole('button', { name: 'Send payment' }).click();
    }
}