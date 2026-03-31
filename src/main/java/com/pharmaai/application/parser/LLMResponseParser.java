package com.pharmaai.application.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaai.domain.model.LLMResponse;

public class LLMResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMResponse parse(String json) {
        try {
            return objectMapper.readValue(json, LLMResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear resposta do LLM", e);
        }
    }
}