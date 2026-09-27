package com.valubank.assistant.exception;

/**
 * Thrown when a tool call to a downstream service fails. Caught per tool
 * call in {@link com.valubank.assistant.service.AssistantService} and
 * reported back to the model as a tool_result error, so the conversation
 * can continue rather than failing the whole request.
 */
public class ToolExecutionException extends RuntimeException {

    public ToolExecutionException(String message) {
        super(message);
    }

    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
