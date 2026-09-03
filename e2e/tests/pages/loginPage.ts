import { Page } from "@playwright/test";

export class LoginPage {

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async open() {
        await this.page.goto('/');
    }

    async loginAs(username: string, password: string) {
        await this.page.getByLabel('Username').fill(username);
        await this.page.getByLabel('Password').fill(password);
        await this.page.getByRole('button', { name: 'Log in' }).click();
    }
}