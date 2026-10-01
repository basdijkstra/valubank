import { Locator, Page } from "@playwright/test";

export class ScheduledPaymentsPage {

    private readonly page: Page;
    readonly successMessageLocator;
    readonly errorMessageLocator;

    constructor(page: Page) {
        this.page = page;
        this.successMessageLocator = this.page.locator('xpath=//div[contains(@class, "banner-success")]');
        this.errorMessageLocator = this.page.locator('xpath=//div[contains(@class, "banner-error")]');
    }

    async open() {
        await this.page.getByRole('link', { name: 'Scheduled payments' }).click();
    }

    async schedulePayment(fromIban: string, recipientIban: string, recipientName: string, amount: string, executionDate: string, description: string) {
        const fromAccount = this.page.getByLabel('From account');
        const accountId = await fromAccount.locator('option', { hasText: fromIban }).getAttribute('value');
        await fromAccount.selectOption(accountId!);
        await this.page.getByLabel('Beneficiary IBAN').fill(recipientIban);
        await this.page.getByLabel('Beneficiary name').fill(recipientName);
        await this.page.getByLabel(/^Amount/).fill(amount);
        await this.page.getByLabel('Execution date').fill(executionDate);
        await this.page.getByLabel('Description').fill(description);
        await this.page.getByRole('button', { name: 'Review payment' }).click();
        await this.page.getByRole('button', { name: 'Confirm' }).click();
    }

    scheduledPayment(description: string): Locator {
        return this.page.locator('.payment-list-item').filter({ hasText: description });
    }

    async scheduledPaymentId(description: string): Promise<string> {
        const testId = await this.scheduledPayment(description).getAttribute('data-testid');
        return testId!.replace('scheduled-payment-', '');
    }

    async cancel(description: string) {
        await this.scheduledPayment(description).getByRole('button', { name: 'Cancel' }).click();
    }
}
