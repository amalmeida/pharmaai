package com.pharmaai.adapter.out.llm;

import com.pharmaai.config.OpenAIConfig;
import com.pharmaai.domain.port.out.LLMPort;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class OpenAILLMAdapter implements LLMPort {

    private final OpenAIConfig config;
    private final RestTemplate restTemplate;

    public OpenAILLMAdapter(OpenAIConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String ask(String prompt) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "Você é um assistente farmacêutico seguro."),
                Map.of("role", "user", "content", prompt)
        );

        body.put("messages", messages);
        body.put("temperature", 0.2);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                config.getUrl(),
                HttpMethod.POST,
                request,
                Map.class
        );

        return extractContent(response.getBody());
    }

    private String extractContent(Map responseBody) {
        List choices = (List) responseBody.get("choices");
        Map choice = (Map) choices.get(0);
        Map message = (Map) choice.get("message");

        return message.get("content").toString();
    }
}