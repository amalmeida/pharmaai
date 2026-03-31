package com.pharmaai.adapter.out.whatsapp;

import com.pharmaai.config.WhatsAppConfig;
import com.pharmaai.domain.port.out.WhatsAppPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class WhatsAppAdapter implements WhatsAppPort {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppAdapter.class);

    private final WhatsAppConfig config;
    private final RestTemplate restTemplate;

    public WhatsAppAdapter(WhatsAppConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendMessage(String to, String text) {

        // Normalizar número brasileiro: webhook envia sem o 9 (ex: 555198745556)
        // mas a API exige com o 9 (ex: 5551998745556)
        to = normalizeBrazilianNumber(to);

        String url = config.getUrl() + "/" + config.getPhoneNumberId() + "/messages";

        log.info("📤 Enviando mensagem WhatsApp para: {} | URL: {}", to, url);
        log.debug("📤 Token (últimos 10 chars): ...{}", 
            config.getToken() != null && config.getToken().length() > 10 
                ? config.getToken().substring(config.getToken().length() - 10) 
                : "NULL/EMPTY");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("✅ WhatsApp API respondeu: status={} | body={}", 
                response.getStatusCode(), response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("❌ WhatsApp API erro HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao enviar mensagem WhatsApp: " + e.getStatusCode() 
                + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao enviar mensagem WhatsApp: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar mensagem WhatsApp", e);
        }
    }

    /**
     * Normaliza números brasileiros para o formato E.164 completo.
     * O webhook do WhatsApp às vezes envia sem o dígito 9 do celular:
     *   webhook: 555198745556  (12 dígitos)
     *   correto: 5551998745556 (13 dígitos)
     *
     * Regra: números BR (55) com 12 dígitos → inserir 9 após o DDD.
     */
    private String normalizeBrazilianNumber(String number) {
        if (number == null) return number;

        // Número BR com 12 dígitos (55 + DDD 2 dígitos + 8 dígitos) → falta o 9
        if (number.startsWith("55") && number.length() == 12) {
            String corrected = "55" + number.substring(2, 4) + "9" + number.substring(4);
            log.info("📱 Número BR normalizado: {} → {}", number, corrected);
            return corrected;
        }

        return number;
    }
}
