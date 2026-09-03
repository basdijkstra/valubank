package com.valubank.payments.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * A single RestTemplate used to call the Accounts Service and the Fraud
 * Service. Short, fixed timeouts so a hung dependency can't hang this
 * service forever - there is no retry/circuit-breaker here on purpose,
 * this is a workshop demo, not production hardening.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
