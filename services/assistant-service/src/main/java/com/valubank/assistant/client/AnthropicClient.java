package com.valubank.assistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.valubank.assistant.exception.AssistantException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Plain HTTP client for the Anthropic Messages API. Request/response bodies
 * are built and read as raw JSON (Jackson tree model) rather than typed
 * DTOs - the tool-use content-block shapes are easier to build this way
 * than modelling every polymorphic variant as a Java class.
 */
@Component
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicClient(RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Value("${valubank.anthropic.api-key}") String apiKey,
                            @Value("${valubank.anthropic.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public JsonNode sendMessage(String systemPrompt, ArrayNode messages, ArrayNode tools) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AssistantException("ANTHROPIC_API_KEY is not configured for assistant-service");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", MAX_TOKENS);
        body.put("system", systemPrompt);
        body.set("messages", messages);
        body.set("tools", tools);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new AssistantException("Anthropic API call failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new AssistantException("Could not reach Anthropic API", e);
        } catch (RestClientException | java.io.IOException e) {
            throw new AssistantException("Anthropic API call failed", e);
        }
    }
}
