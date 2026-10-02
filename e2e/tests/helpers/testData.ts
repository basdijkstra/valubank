import { APIRequestContext } from "@playwright/test";
import { AccountsApi } from "./accountsApi";
import { PaymentsApi } from "./paymentsApi";

/** An id no other test run uses. */
export function uniqueId(): string {
    return `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
}

/**
 * Cleans up the scheduled payments of Alice (1) and Bob (2) that contain the
 * run id: cancels the ones still SCHEDULED, so the background run doesn't
 * execute them later, and credits back the amount of the EXECUTED ones, so
 * balances don't drift down from run to run.
 */
export async function cleanUpScheduledPayments(request: APIRequestContext, runId: string) {
    const paymentsApi = new PaymentsApi(request);
    const accountsApi = new AccountsApi(request);
    for (const customerId of [1, 2]) {
        for (const scheduledPayment of await paymentsApi.findScheduledPayments(customerId, runId)) {
            if (scheduledPayment.status === 'SCHEDULED') {
                await paymentsApi.cancelScheduledPayment(scheduledPayment.id, customerId);
            } else if (scheduledPayment.status === 'EXECUTED') {
                await accountsApi.credit(scheduledPayment.fromAccountId, String(scheduledPayment.amount));
            }
        }
    }
}
