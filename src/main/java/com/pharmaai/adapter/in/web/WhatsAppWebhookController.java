package com.pharmaai.adapter.in.web;

import com.pharmaai.application.usecase.WhatsAppMessageProcessor;
import com.pharmaai.config.WhatsAppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppConfig whatsAppConfig;
    private final WhatsAppMessageProcessor messageProcessor;

    // Deduplicação simples: evita processar a mesma mensagem duas vezes
    private final Set<String> processedMessages = ConcurrentHashMap.newKeySet();

    public WhatsAppWebhookController(WhatsAppConfig whatsAppConfig,
                                      WhatsAppMessageProcessor messageProcessor) {
        this.whatsAppConfig = whatsAppConfig;
        this.messageProcessor = messageProcessor;
    }

    /**
     * GET /webhook — Verificação do webhook pelo Meta.
     * O Meta envia: hub.mode, hub.verify_token, hub.challenge
     * Se o token bater, retornamos o challenge.
     */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && whatsAppConfig.getVerifyToken().equals(verifyToken)) {
            log.info("✅ Webhook verificado com sucesso!");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ Falha na verificação do webhook");
        return ResponseEntity.status(403).body("Verification failed");
    }

    /**
     * POST /webhook — Recebe mensagens do WhatsApp.
     * Retorna 200 imediatamente (Meta exige resposta rápida).
     * Processa a mensagem de forma assíncrona via WhatsAppMessageProcessor.
     */
    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody JsonNode payload) {

        log.info("📨 Webhook POST recebido: {}", payload.toString().substring(0, Math.min(200, payload.toString().length())));

        try {
            extractAndProcess(payload);
        } catch (Exception e) {
            log.error("⚠️ Erro ao extrair mensagem do payload: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Extrai dados da mensagem e dispara processamento assíncrono.
     */
    private void extractAndProcess(JsonNode payload) {

        JsonNode entry = payload.path("entry");
        if (entry.isMissingNode() || !entry.isArray() || entry.isEmpty()) {
            log.debug("⏭️ Payload sem entry");
            return;
        }

        JsonNode changes = entry.get(0).path("changes");
        if (changes.isMissingNode() || !changes.isArray() || changes.isEmpty()) {
            log.debug("⏭️ Payload sem changes");
            return;
        }

        JsonNode value = changes.get(0).path("value");

        JsonNode messages = value.path("messages");
        if (messages.isMissingNode() || !messages.isArray() || messages.isEmpty()) {
            log.debug("⏭️ Sem mensagens (pode ser status update)");
            return;
        }

        JsonNode message = messages.get(0);
        String messageId = message.path("id").asText("");
        String from = message.path("from").asText("");
        String type = message.path("type").asText("");

        if (!"text".equals(type)) {
            log.info("⏭️ Mensagem ignorada (tipo: {})", type);
            return;
        }

        String text = message.path("text").path("body").asText("");

        if (text.isBlank()) return;

        // Deduplicação
        if (!processedMessages.add(messageId)) {
            log.info("⏭️ Mensagem duplicada ignorada: {}", messageId);
            return;
        }

        if (processedMessages.size() > 1000) {
            processedMessages.clear();
        }

        log.info("📩 WhatsApp de {}: {}", from, text);

        // Processar de forma assíncrona (chamada em outro bean = @Async funciona!)
        messageProcessor.processAsync(from, text);

        log.info("🚀 Processamento async disparado para {}", from);
    }
}
