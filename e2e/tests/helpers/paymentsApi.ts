import { APIRequestContext, APIResponse } from "@playwright/test";
import { isoDate } from "./dates";

const PAYMENTS_SERVICE_URL = 'http://localhost:8082';

/** Body of POST /api/scheduled-payments. Fields are optional so tests can leave them out on purpose. */
export interface ScheduledPaymentRequest {
    customerId?: number;
    fromAccountId?: number;
    toAccountIban?: string;
    toAccountName?: string;
    amount?: number | string;
    executionDate?: string;
    description?: string;
}

/** A scheduled payment as returned by the Payments Service. */
export interface ScheduledPayment {
    id: number;
    customerId: number;
    fromAccountId: number;
    toAccountIban: string;
    toAccountName: string;
    amount: number;
    currency: string;
    description: string;
    executionDate: string;
    status: string;
    reason: string | null;
    paymentId: number | null;
    createdAt: string;
    executedAt: string | null;
    cancelledAt: string | null;
}

/** A payment from an account's payment history. */
export interface Payment {
    id: number;
    fromAccountId: number;
    toAccountIban: string;
    toAccountName: string;
    amount: number;
    currency: string;
    description: string;
    status: string;
    reason: string | null;
    timestamp: string;
}

/**
 * Direct calls to the Payments Service. The raw calls (schedulePayment,
 * cancelScheduledPayment, executeScheduledPayment) return the response, so
 * tests can assert on error statuses. The other methods are for setup and
 * lookups, and throw when the call fails.
 */
export class PaymentsApi {

    private readonly request: APIRequestContext;

    constructor(request: APIRequestContext) {
        this.request = request;
    }

    async schedulePayment(body: ScheduledPaymentRequest | object): Promise<APIResponse> {
        return await this.request.post(`${PAYMENTS_SERVICE_URL}/api/scheduled-payments`, { data: body });
    }

    async cancelScheduledPayment(scheduledPaymentId: number, customerId?: number): Promise<APIResponse> {
        return await this.request.post(
            `${PAYMENTS_SERVICE_URL}/api/scheduled-payments/${scheduledPaymentId}/cancel`,
            customerId === undefined ? {} : { params: { customerId } });
    }

    /** TEST-ONLY endpoint: executes a scheduled payment now, ignoring its execution date. */
    async executeScheduledPayment(scheduledPaymentId: number): Promise<APIResponse> {
        return await this.request.post(`${PAYMENTS_SERVICE_URL}/api/test-support/scheduled-payments/${scheduledPaymentId}/execute`);
    }

    /**
     * Schedules a valid payment (Alice's checking to Bob, 1.00, tomorrow),
     * with the given fields overridden.
     */
    async scheduleValidPayment(overrides: ScheduledPaymentRequest): Promise<ScheduledPayment> {
        const response = await this.schedulePayment({
            customerId: 1,
            fromAccountId: 1,
            toAccountIban: 'NL01VALU0000000003',
            toAccountName: 'Bob de Vries',
            amount: 1.00,
            executionDate: isoDate(1),
            ...overrides
        });
        if (response.status() !== 201) {
            throw new Error(`Scheduling a payment failed with status ${response.status()}: ${await response.text()}`);
        }
        return await response.json();
    }

    async getScheduledPayments(customerId: number): Promise<ScheduledPayment[]> {
        const response = await this.request.get(`${PAYMENTS_SERVICE_URL}/api/customers/${customerId}/scheduled-payments`);
        if (!response.ok()) {
            throw new Error(`GET scheduled payments of customer ${customerId} failed with status ${response.status()}`);
        }
        return await response.json();
    }

    /** The customer's scheduled payments whose description or beneficiary name contains the text. */
    async findScheduledPayments(customerId: number, text: string): Promise<ScheduledPayment[]> {
        return (await this.getScheduledPayments(customerId)).filter((scheduledPayment) =>
            (scheduledPayment.description ?? '').includes(text) || (scheduledPayment.toAccountName ?? '').includes(text));
    }

    async getPaymentsForAccount(accountId: number): Promise<Payment[]> {
        const response = await this.request.get(`${PAYMENTS_SERVICE_URL}/api/accounts/${accountId}/payments`);
        if (!response.ok()) {
            throw new Error(`GET payments of account ${accountId} failed with status ${response.status()}`);
        }
        return await response.json();
    }
}
