package com.valubank.assistant.controller;

import com.valubank.assistant.dto.ChatMessageDto;
import com.valubank.assistant.dto.ChatRequest;
import com.valubank.assistant.dto.ChatResponse;
import com.valubank.assistant.service.AssistantService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/api/assistant/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        List<ChatMessageDto> history = request.getHistory() != null ? request.getHistory() : List.of();
        String reply = assistantService.chat(request.getCustomerId(), request.getMessage(), history);
        return new ChatResponse(reply);
    }
}
