import { APIRequestContext } from "@playwright/test";
import currency from 'currency.js';

const ACCOUNTS_SERVICE_URL = 'http://localhost:8081';

/**
 * Direct calls to the Accounts Service: reading balances, and setting them up
 * (or restoring them) through the internal balance-mutations endpoint.
 */
export class AccountsApi {

    private readonly request: APIRequestContext;

    constructor(request: APIRequestContext) {
        this.request = request;
    }

    async getBalance(accountId: number): Promise<string> {
        const response = await this.request.get(`${ACCOUNTS_SERVICE_URL}/api/accounts/${accountId}`);
        if (!response.ok()) {
            throw new Error(`GET account ${accountId} failed with status ${response.status()}`);
        }
        const account = await response.json();
        return String(account.balance);
    }

    async credit(accountId: number, amount: string) {
        await this.mutateBalance(accountId, 'CREDIT', amount);
    }

    async debit(accountId: number, amount: string) {
        await this.mutateBalance(accountId, 'DEBIT', amount);
    }

    /** Credits or debits the difference, so the account ends at exactly the target balance. */
    async setBalance(accountId: number, targetBalance: string) {
        const difference = currency(targetBalance).subtract(await this.getBalance(accountId));
        if (difference.value > 0) {
            await this.credit(accountId, difference.toString());
        } else if (difference.value < 0) {
            await this.debit(accountId, difference.multiply(-1).toString());
        }
    }

    private async mutateBalance(accountId: number, type: string, amount: string) {
        const response = await this.request.post(`${ACCOUNTS_SERVICE_URL}/api/accounts/${accountId}/balance-mutations`, {
            data: { type, amount: currency(amount).value, reason: 'E2E test data setup' }
        });
        if (!response.ok()) {
            throw new Error(`${type} of ${amount} on account ${accountId} failed with status ${response.status()}`);
        }
    }
}
