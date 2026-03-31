package com.pharmaai.application.usecase;

import com.pharmaai.application.parser.LLMResponseParser;
import com.pharmaai.application.prompt.PromptBuilderV2;
import com.pharmaai.application.rag.DrugKnowledgeService;
import com.pharmaai.domain.model.LLMResponse;
import com.pharmaai.domain.port.out.LLMPort;
import com.pharmaai.domain.port.out.WhatsAppPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageProcessor.class);

    private final DrugKnowledgeService knowledgeService;
    private final PromptBuilderV2 promptBuilder;
    private final LLMPort llmPort;
    private final LLMResponseParser parser;
    private final WhatsAppPort whatsAppPort;

    public WhatsAppMessageProcessor(DrugKnowledgeService knowledgeService,
                                     PromptBuilderV2 promptBuilder,
                                     LLMPort llmPort,
                                     LLMResponseParser parser,
                                     WhatsAppPort whatsAppPort) {
        this.knowledgeService = knowledgeService;
        this.promptBuilder = promptBuilder;
        this.llmPort = llmPort;
        this.parser = parser;
        this.whatsAppPort = whatsAppPort;
    }

    @Async
    public void processAsync(String from, String userInput) {
        try {
            log.info("🔄 Processando mensagem de {}: {}", from, userInput);

            // 1. Buscar contexto RAG
            String context = knowledgeService.getContext(userInput);
            log.info("📚 Contexto obtido ({} chars): {}", 
                context != null ? context.length() : 0,
                context != null ? context.substring(0, Math.min(200, context.length())) : "null");

            // 2. Construir prompt
            String prompt = promptBuilder.buildWithContext(userInput, context);
            log.debug("📝 Prompt construído ({} chars)", prompt.length());

            // 3. Chamar LLM
            log.info("🤖 Chamando LLM...");
            String raw = llmPort.ask(prompt);
            log.info("🤖 LLM respondeu ({} chars): {}", 
                raw != null ? raw.length() : 0,
                raw != null ? raw.substring(0, Math.min(200, raw.length())) : "null");

            // 4. Parsear resposta
            LLMResponse response = parser.parse(raw);
            log.info("✅ Parse OK - risk: {}", response.getRisk_level());

            // 5. Montar mensagem para WhatsApp
            String reply = formatReply(response);

            // 6. Enviar resposta
            log.info("📤 Enviando resposta para {}...", from);
            whatsAppPort.sendMessage(from, reply);
            log.info("✅ Resposta enviada para {}", from);

        } catch (Exception e) {
            log.error("❌ Erro ao processar mensagem de {}: {}", from, e.getMessage(), e);

            try {
                whatsAppPort.sendMessage(from,
                        "⚠️ Desculpe, ocorreu um erro ao processar sua pergunta. Tente novamente em alguns instantes.");
            } catch (Exception ex) {
                log.error("❌ Erro ao enviar mensagem de erro: {}", ex.getMessage(), ex);
            }
        }
    }

    private String formatReply(LLMResponse response) {
        StringBuilder sb = new StringBuilder();

        if (response.getAnswer() != null) {
            sb.append("💊 *Resposta:*\n");
            sb.append(response.getAnswer()).append("\n\n");
        }

        if (response.getRisk_level() != null) {
            String emoji = switch (response.getRisk_level().toUpperCase()) {
                case "HIGH" -> "🔴";
                case "MEDIUM" -> "🟡";
                case "LOW" -> "🟢";
                default -> "⚪";
            };
            sb.append(emoji).append(" *Risco:* ").append(response.getRisk_level()).append("\n\n");
        }

        if (response.getAlerts() != null && !response.getAlerts().isEmpty()) {
            sb.append("⚠️ *Alertas:*\n");
            for (String alert : response.getAlerts()) {
                sb.append("• ").append(alert).append("\n");
            }
            sb.append("\n");
        }

        if (response.getRecommendation() != null) {
            sb.append("👨‍⚕️ *Recomendação:*\n");
            sb.append(response.getRecommendation()).append("\n");
        }

        sb.append("\n_PharmaAI — Sempre consulte um profissional de saúde._");

        return sb.toString();
    }
}
