package com.pharmaai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class StartupValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    private final OpenAIConfig openAIConfig;
    private final WhatsAppConfig whatsAppConfig;

    public StartupValidator(OpenAIConfig openAIConfig, WhatsAppConfig whatsAppConfig) {
        this.openAIConfig = openAIConfig;
        this.whatsAppConfig = whatsAppConfig;
    }

    @Override
    public void run(String... args) {
        log.info("🔍 Validando configuração...");

        boolean ok = true;

        // OpenAI
        if (isBlank(openAIConfig.getKey())) {
            log.error("❌ OPENAI_API_KEY não configurada! LLM não funcionará.");
            ok = false;
        } else {
            String masked = openAIConfig.getKey().substring(0, Math.min(10, openAIConfig.getKey().length())) + "...";
            log.info("✅ OpenAI API Key: {}", masked);
        }

        // WhatsApp Token
        if (isBlank(whatsAppConfig.getToken())) {
            log.error("❌ WHATSAPP_TOKEN não configurado! Envio de mensagens não funcionará.");
            ok = false;
        } else {
            String masked = whatsAppConfig.getToken().substring(0, Math.min(10, whatsAppConfig.getToken().length())) + "...";
            log.info("✅ WhatsApp Token: {}", masked);
        }

        // WhatsApp Phone Number ID
        if (isBlank(whatsAppConfig.getPhoneNumberId())) {
            log.warn("⚠️ WHATSAPP_PHONE_NUMBER_ID não configurado. Usando default.");
        } else {
            log.info("✅ WhatsApp Phone ID: {}", whatsAppConfig.getPhoneNumberId());
        }

        // Verify Token
        log.info("✅ WhatsApp Verify Token: {}", whatsAppConfig.getVerifyToken());

        if (ok) {
            log.info("🚀 Configuração OK — PharmaAI pronto para uso!");
        } else {
            log.warn("⚠️ Configuração incompleta — algumas funcionalidades podem falhar.");
            log.warn("💡 Configure via variáveis de ambiente:");
            log.warn("   $env:OPENAI_API_KEY = \"sk-proj-...\"");
            log.warn("   $env:WHATSAPP_TOKEN = \"EAAw...\"");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank() || value.equals("${OPENAI_API_KEY}") || value.equals("${WHATSAPP_TOKEN}");
    }
}

