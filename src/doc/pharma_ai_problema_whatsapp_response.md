# 💊 PharmaAI — Problema: Resposta não chega ao WhatsApp

> **Data:** 2026-03-31  
> **Status:** 🟡 Fluxo E2E funcional, resposta não retorna ao WhatsApp

---

## ❌ Problema Atual

### Sintoma
A mensagem é recebida do WhatsApp, processada com sucesso (RAG + LLM), mas **a resposta NÃO chega de volta** ao usuário no WhatsApp.

### Logs observados (parecem OK, mas escondem o erro)
```
📨 Webhook POST recebido
📩 WhatsApp de 555198745556: Posso tomar 5g de paracetamol?
🚀 Processamento async disparado
🔄 Processando mensagem...
📚 Contexto obtido (2524 chars)
🤖 LLM respondeu (544 chars)
✅ Parse OK - risk: HIGH
📤 Enviando resposta para 555198745556...
✅ Resposta enviada para 555198745556     ← LOGS DIZEM QUE FOI! MAS NÃO FOI.
```

---

## 🔍 Causa Raiz Identificada (2 problemas)

### 1. Token do WhatsApp expirado (401 Unauthorized)

Testando diretamente via curl:
```bash
curl -X POST "https://graph.facebook.com/v22.0/979436765263547/messages" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"messaging_product":"whatsapp","to":"5551998745556","type":"text","text":{"body":"teste"}}'
```

Resposta da API:
```json
{
  "error": {
    "message": "Error validating access token: Session has expired on Tuesday, 31-Mar-26 06:00:00 PDT",
    "type": "OAuthException",
    "code": 190,
    "error_subcode": 463
  }
}
```

### 2. WhatsAppAdapter engolia exceções silenciosamente

**Código ANTES (problemático):**
```java
try {
    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    System.out.println("📤 enviada");  // ← stdout, NÃO aparece no log file!
} catch (Exception e) {
    System.out.println("❌ Erro");     // ← engolia a exceção!
    e.printStackTrace();               // ← NÃO relançava!
}
```

**Problemas:**
- `System.out.println` não vai para o arquivo `pharmaai.log` (só console)
- Exceção capturada e **NÃO relançada** → `WhatsAppMessageProcessor` pensa que deu certo
- Log "✅ Resposta enviada" era falso positivo

---

## ✅ Correções Aplicadas

### WhatsAppAdapter — Corrigido
- ✅ `System.out.println` → SLF4J Logger
- ✅ Log do status + body da resposta da API Meta
- ✅ Exceções relançadas como `RuntimeException` 
- ✅ Log do token (últimos 10 chars) para debug
- ✅ Tratamento específico de `HttpClientErrorException`

### Token — Precisa ser renovado manualmente
- ⚠️ Tokens temporários do Meta expiram em ~24h
- Passos:
  1. Acesse [Meta for Developers](https://developers.facebook.com/)
  2. WhatsApp > API Setup > **Generate Token**
  3. Atualize em `application.properties`
  4. Reinicie a aplicação

---

## 📊 Fluxo do Sistema

```
[Usuário WhatsApp]
    ↓ envia mensagem
[Meta Cloud API] → POST /webhook
[WhatsAppWebhookController] → retorna 200 imediato
    ↓ @Async
[WhatsAppMessageProcessor]
    ├─ DrugNameExtractor → match rápido local
    ├─ DrugNameNormalizer → LLM normaliza nome
    ├─ DailyMedClient → busca RAG (XML SPL)
    ├─ PromptBuilderV2 → monta prompt
    ├─ OpenAILLMAdapter → GPT-4o-mini
    ├─ LLMResponseParser → parse JSON
    └─ WhatsAppAdapter → envia resposta ← ❌ FALHA (token expirado)
```

---

## 🚀 Após Renovar Token

1. Atualizar `application.properties` com novo token
2. Reiniciar aplicação
3. Enviar mensagem teste via WhatsApp
4. Verificar `target/pharmaai.log` — agora mostrará erros HTTP se houver
5. Confirmar que resposta chega no WhatsApp

