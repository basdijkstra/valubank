package com.valubank.assistant.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * A single RestTemplate used for calls to the Accounts Service, Payments
 * Service, and the Anthropic API. The read timeout is generous (LLM calls
 * with tool use can take several seconds) - there is no retry/circuit-breaker
 * here on purpose, this is a workshop demo, not production hardening.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
