import { Page } from "@playwright/test";

/**
 * Changes what the browser receives from the services. Only affects the
 * page's own requests, not the API helpers.
 */

/** The page can't load the customer's accounts, as if the Accounts Service were down. */
export async function failAccountsRequests(page: Page) {
    await page.route('**/api/customers/*/accounts', (route) => route.abort('connectionrefused'));
}

/** Scheduling a payment takes a while. */
export async function delayScheduleRequest(page: Page, delayMs = 1500) {
    await page.route('**/api/scheduled-payments', async (route) => {
        if (route.request().method() === 'POST') {
            await new Promise((resolve) => setTimeout(resolve, delayMs));
        }
        await route.continue();
    });
}
