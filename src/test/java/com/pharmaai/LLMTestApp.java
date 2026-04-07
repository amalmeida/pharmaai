package com.pharmaai;

/**
 * ⚠️ DEPRECADO — Após Fase 4 (PostgreSQL), o teste manual deve ser feito:
 *
 * 1. Subir PostgreSQL: docker compose up -d
 * 2. Rodar a aplicação: ./mvnw spring-boot:run
 * 3. Testar via curl:
 *    curl -X POST http://localhost:8080/webhook \
 *      -H "Content-Type: application/json" \
 *      -d '{"entry":[{"changes":[{"value":{"messages":[{"id":"test1","from":"5551999999999","type":"text","text":{"body":"Posso tomar 5g de paracetamol?"}}]}}]}]}'
 * 4. Ou testar via WhatsApp (com ngrok ativo)
 *
 * O HybridDrugInfoProvider agora depende de JPA (PostgreSQL),
 * não é mais possível rodar sem o contexto Spring Boot.
 */
public class LLMTestApp {

    public static void main(String[] args) {
        System.out.println("⚠️ Este teste está deprecado após a Fase 4 (PostgreSQL).");
        System.out.println("Use: ./mvnw spring-boot:run + curl ou WhatsApp para testar.");
        System.out.println("Veja instruções no README.md");
    }
}