package com.valubank.assistant.dto;

/**
 * One turn of prior conversation, as kept by the frontend. There is no
 * server-side conversation store: the frontend resends the full history it
 * holds in memory on every request.
 */
public class ChatMessageDto {

    /** "user" or "assistant" */
    private String role;
    private String content;

    public ChatMessageDto() {
    }

    public ChatMessageDto(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
