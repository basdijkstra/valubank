package com.valubank.assistant.exception;

/**
 * Thrown when the call to the LLM provider itself fails (missing/invalid API
 * key, network failure, malformed response). Unlike {@link ToolExecutionException},
 * this ends the request - there is no model to hand a tool error back to.
 */
public class AssistantException extends RuntimeException {

    public AssistantException(String message) {
        super(message);
    }

    public AssistantException(String message, Throwable cause) {
        super(message, cause);
    }
}
