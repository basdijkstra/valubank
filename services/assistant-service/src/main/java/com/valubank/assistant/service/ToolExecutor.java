package com.valubank.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.valubank.assistant.client.AccountsServiceClient;
import com.valubank.assistant.client.PaymentsServiceClient;
import com.valubank.assistant.exception.ToolExecutionException;
import org.springframework.stereotype.Component;

/**
 * Executes the tool calls the model asks for.
 *
 * Deliberately does not check whether the customerId/accountId the model
 * supplies belongs to the customer the current conversation is scoped to -
 * that check does not exist anywhere in this service. The only thing
 * telling the model to stay within the current customer's own data is the
 * system prompt built in {@link AssistantService}.
 */
@Component
public class ToolExecutor {

    private final AccountsServiceClient accountsServiceClient;
    private final PaymentsServiceClient paymentsServiceClient;

    public ToolExecutor(AccountsServiceClient accountsServiceClient, PaymentsServiceClient paymentsServiceClient) {
        this.accountsServiceClient = accountsServiceClient;
        this.paymentsServiceClient = paymentsServiceClient;
    }

    public String execute(String toolName, JsonNode input) {
        switch (toolName) {
            case "list_accounts":
                return accountsServiceClient.listAccountsForCustomer(requireLong(input, "customerId"));
            case "get_account":
                return accountsServiceClient.getAccount(requireLong(input, "accountId"));
            case "get_interest_rate":
                return accountsServiceClient.getInterestRate(requireLong(input, "accountId"));
            case "list_payments":
                return paymentsServiceClient.listPaymentsForAccount(requireLong(input, "accountId"));
            default:
                throw new ToolExecutionException("Unknown tool: " + toolName);
        }
    }

    private Long requireLong(JsonNode input, String field) {
        JsonNode value = input.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new ToolExecutionException("Missing or invalid '" + field + "' argument");
        }
        return value.asLong();
    }
}
