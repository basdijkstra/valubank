import { Locator, Page } from "@playwright/test";

export class ScheduledPaymentsPage {

    private readonly page: Page;
    readonly fromAccountLocator: Locator;
    readonly fromAccountOptionsLocator: Locator;
    readonly recipientIbanLocator: Locator;
    readonly recipientNameLocator: Locator;
    readonly amountLocator: Locator;
    readonly executionDateLocator: Locator;
    readonly descriptionLocator: Locator;
    readonly reviewButtonLocator: Locator;
    readonly confirmButtonLocator: Locator;
    readonly confirmationLocator: Locator;
    readonly formErrorLocator: Locator;
    readonly errorMessageLocator: Locator;
    readonly successMessageLocator: Locator;
    readonly emptyListMessageLocator: Locator;
    readonly entriesLocator: Locator;

    constructor(page: Page) {
        this.page = page;
        this.fromAccountLocator = this.page.getByLabel('From account');
        this.fromAccountOptionsLocator = this.fromAccountLocator.locator('option');
        this.recipientIbanLocator = this.page.getByLabel('Beneficiary IBAN');
        this.recipientNameLocator = this.page.getByLabel('Beneficiary name');
        this.amountLocator = this.page.getByLabel('Amount');
        this.executionDateLocator = this.page.getByLabel('Execution date');
        this.descriptionLocator = this.page.getByLabel('Description');
        this.reviewButtonLocator = this.page.getByRole('button', { name: 'Review payment' });
        this.confirmButtonLocator = this.page.locator('.scheduled-confirm .btn-primary');
        this.confirmationLocator = this.page.locator('.scheduled-confirm');
        this.formErrorLocator = this.page.locator('.form-error');
        this.errorMessageLocator = this.page.locator('xpath=//div[contains(@class, "banner-error") and not(contains(@class, "form-error"))]');
        this.successMessageLocator = this.page.locator('xpath=//div[contains(@class, "banner-success")]');
        this.emptyListMessageLocator = this.page.getByText('No scheduled payments yet.');
        this.entriesLocator = this.page.locator('.payment-list-item');
    }

    async open(accountId?: string) {
        await this.page.goto(accountId ? `/scheduled-payments?accountId=${accountId}` : '/scheduled-payments');
    }

    async reload() {
        await this.page.reload();
    }

    async selectFromAccount(iban: string) {
        const option = this.fromAccountLocator.locator('option', { hasText: iban });
        await option.waitFor({ state: 'attached' });
        await this.fromAccountLocator.selectOption(await option.getAttribute('value') ?? '');
    }

    async reviewPayment(recipientIban: string, recipientName: string, amount: string, executionDate: string, description: string) {
        await this.recipientIbanLocator.fill(recipientIban);
        await this.recipientNameLocator.fill(recipientName);
        await this.amountLocator.fill(amount);
        await this.executionDateLocator.fill(executionDate);
        await this.descriptionLocator.fill(description);
        await this.reviewButtonLocator.click();
    }

    async confirmPayment() {
        await this.confirmButtonLocator.click();
        await this.successMessageLocator.waitFor({ state: 'visible' });
    }

    async schedulePayment(recipientIban: string, recipientName: string, amount: string, executionDate: string, description: string) {
        await this.reviewPayment(recipientIban, recipientName, amount, executionDate, description);
        await this.confirmPayment();
    }

    async goBack() {
        await this.page.getByRole('button', { name: 'Back' }).click();
    }

    async reviewWithAmount(amount: string) {
        await this.amountLocator.fill(amount);
        await this.reviewButtonLocator.click();
    }

    async doubleClickConfirm() {
        await this.confirmButtonLocator.dblclick();
    }

    async enterRecipientIban(recipientIban: string) {
        await this.recipientIbanLocator.fill(recipientIban);
    }

    async cancelScheduledPayment(text: string) {
        await this.cancelButtonLocatorFor(text).click();
    }

    async getDescriptionsInListOrder(): Promise<string[]> {
        await this.entriesLocator.first().waitFor({ state: 'visible' });
        return await this.entriesLocator.locator('.payment-description').allInnerTexts();
    }

    summaryValueLocatorFor(label: string): Locator {
        return this.page.locator(`xpath=//div[@class='account-summary-row' and span[@class='label' and text()='${label}']]/span[2]`);
    }

    entryLocatorFor(text: string): Locator {
        return this.page.locator(`xpath=//li[contains(@class, 'payment-list-item') and .//*[normalize-space(text())='${text}']]`);
    }

    statusLocatorFor(text: string): Locator {
        return this.entryLocatorFor(text).locator('.payment-status');
    }

    reasonLocatorFor(text: string): Locator {
        return this.entryLocatorFor(text).locator('.payment-reason');
    }

    descriptionLocatorFor(text: string): Locator {
        return this.entryLocatorFor(text).locator('.payment-description');
    }

    cancelButtonLocatorFor(text: string): Locator {
        return this.entryLocatorFor(text).getByRole('button', { name: 'Cancel' });
    }
}
