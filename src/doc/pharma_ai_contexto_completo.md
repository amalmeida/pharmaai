# 💊 PharmaAI — Documento de Contexto

> **Data:** 2026-03-31  
> **Status:** ✅ E2E Funcionando (WhatsApp → RAG → LLM → WhatsApp)

---

## 🎯 Objetivo do Sistema

Aplicação em **Java (Spring Boot + Arquitetura Hexagonal)** para validação de prescrições médicas e suporte farmacêutico usando **IA Generativa (LLM + RAG)**.

**Entrada:** Pergunta em linguagem natural via WhatsApp  
**Saída:** Resposta estruturada com risco, alertas e recomendação

### Exemplo
- **Pergunta:** "Posso tomar 5g de paracetamol?"
- **Resposta:** 🔴 HIGH — Dose máxima é 4g/dia, risco de dano hepático

---

## 🏗️ Arquitetura (Hexagonal)

```
domain/          → Regras e contratos (ports)
  model/         → DrugInfo, LLMResponse
  port/out/      → LLMPort, WhatsAppPort

application/     → Casos de uso
  parser/        → LLMResponseParser
  prompt/        → PromptBuilderV2
  rag/           → DrugKnowledgeService, DrugNameExtractor,
                   DrugNameNormalizer, ContextFormatter, DrugInfoProvider
  usecase/       → WhatsAppMessageProcessor (@Async)

adapter/         → Integrações externas
  in/web/        → WhatsAppWebhookController
  out/llm/       → OpenAILLMAdapter (GPT-4o-mini)
  out/whatsapp/  → WhatsAppAdapter (Meta Cloud API)
  rag/           → DailyMedClient, DailyMedMapper,
                   DrugCacheRepository, HybridDrugInfoProvider

config/          → AppConfig, OpenAIConfig, WhatsAppConfig
```

---

## 🔄 Fluxo Completo

```
[Usuário WhatsApp]
    ↓ envia mensagem
[Meta Cloud API]
    ↓ POST /webhook
[WhatsAppWebhookController]
    ↓ retorna 200 imediato ao Meta
    ↓ dispara @Async
[WhatsAppMessageProcessor] (thread separada)
    ├─ 1. DrugNameExtractor     → match rápido no cache (custo zero)
    ├─ 2. DrugNameNormalizer    → LLM resolve nome se necessário
    ├─ 3. DailyMedClient        → busca contexto RAG (XML HL7 SPL)
    ├─ 4. DailyMedMapper        → extrai seções relevantes
    ├─ 5. PromptBuilderV2       → monta prompt com contexto + regras
    ├─ 6. OpenAILLMAdapter      → chama GPT-4o-mini
    ├─ 7. LLMResponseParser     → converte resposta JSON
    └─ 8. WhatsAppAdapter       → envia resposta formatada
[Meta Cloud API]
    ↓
[Usuário WhatsApp] ← resposta com emoji de risco, alertas, recomendação
```

**Tempo médio:** ~8 segundos

---

## ✅ O que já foi implementado

### RAG (Retrieval-Augmented Generation)
- ✅ `DailyMedClient` — Busca medicamentos na API DailyMed (XML)
- ✅ `DailyMedMapper` — Parse XML HL7 SPL (seções aninhadas)
- ✅ `DrugCacheRepository` — Cache local em JSON
- ✅ `HybridDrugInfoProvider` — Cache + DailyMed com fallback
- ✅ `DrugNameNormalizer` — LLM resolve nomes (tilenol → acetaminophen)
- ✅ `DrugNameExtractor` — Match rápido local (custo zero)
- ✅ Sinônimos PT-BR → inglês/US no DailyMedClient
- ✅ Múltiplos setIds com fallback

### LLM
- ✅ `OpenAILLMAdapter` — GPT-4o-mini
- ✅ `PromptBuilderV2` — Prompts com contexto + regras adulto/infantil
- ✅ `LLMResponseParser` — Parse JSON estruturado

### WhatsApp
- ✅ `WhatsAppWebhookController` — Webhook GET/POST
- ✅ `WhatsAppAdapter` — Envio via Meta Cloud API
- ✅ Processamento assíncrono (@Async em bean separado)
- ✅ Deduplicação de mensagens
- ✅ Formatação de resposta (risco com emoji, alertas, recomendação)
- ✅ Logging via SLF4J (arquivo + console)

### Teste E2E ✅
- ✅ WhatsApp → Webhook → RAG → LLM → WhatsApp
- ✅ Testado com "Posso tomar 5g de paracetamol?" → Risco HIGH

---

## 🔧 Problemas Resolvidos

| # | Problema | Solução |
|---|---|---|
| 1 | Erro 415 no DailyMed | Endpoint `.json` não existe para SPL; usar `.xml` |
| 2 | Erro 406 no DailyMed | Header `Accept: */*` |
| 3 | SPL sem texto (produtos internacionais) | Fallback por múltiplos setIds |
| 4 | "paracetamol" não encontrado | Mapa de sinônimos PT-BR → US |
| 5 | "tilenol sinos" não encontrado | `DrugNameNormalizer` via LLM |
| 6 | Sem info de idade do paciente | PromptBuilderV2 traz adulto + infantil |
| 7 | `@Async` não funcionava | Movido para bean separado (`WhatsAppMessageProcessor`) |
| 8 | Logs não apareciam no arquivo | Migrado `System.out.println` → SLF4J Logger |
| 9 | Código morto acumulado | Limpeza de 8 arquivos não utilizados |
| 10 | Resposta não chegava ao WhatsApp | Token expirado + adapter engolia exceções silenciosamente |
| 11 | `System.out.println` restantes | Migrado 100% dos arquivos de produção para SLF4J |

---

## 🧹 Código Removido (Limpeza 31/03/2026)

| Arquivo | Motivo |
|---|---|
| `ProcessMessageService.java` | Substituído por `WhatsAppMessageProcessor` |
| `PromptBuilder.java` | Substituído por `PromptBuilderV2` |
| `PrescriptionParser.java` | Nunca utilizado |
| `Prescription.java` | Nunca utilizado |
| `DrugKnowledgeBase.java` | Dados hardcoded, substituído pelo RAG |
| `LocalJsonDrugInfoProvider.java` | Substituído por `HybridDrugInfoProvider` |
| `adapter/OpenAILLMAdapter.java` | Duplicado (mantido em `adapter/out/llm/`) |
| `src/application.yml` | Fora do classpath Spring Boot |

---

## 📍 Configuração Atual

### application.properties
```properties
# OpenAI
openai.api.key=${OPENAI_API_KEY:sk-proj-...}
openai.api.url=https://api.openai.com/v1/chat/completions
openai.api.model=gpt-4o-mini

# WhatsApp Business API
whatsapp.api.token=${WHATSAPP_TOKEN:...}
whatsapp.api.phone-number-id=979436765263547
whatsapp.api.verify-token=pharmaai-verify-2024
whatsapp.api.url=https://graph.facebook.com/v22.0

# Logging
logging.file.name=target/pharmaai.log
logging.level.com.pharmaai=DEBUG
```

### Infraestrutura
- **ngrok:** `https://willis-nonretractile-geminally.ngrok-free.dev`
- **Porta local:** 8080
- **Número WhatsApp teste:** +1 555 174 9365
- **Número destinatário:** +55 51 99874 5556

---

## 🚀 Roadmap

### ✅ Concluído
- [x] RAG com DailyMed (XML HL7 SPL)
- [x] Integração OpenAI (GPT-4o-mini)
- [x] WhatsApp Business API (webhook + envio)
- [x] Teste E2E completo
- [x] Limpeza de código morto

### Curto prazo
- [ ] Token permanente (System User no Meta)
- [ ] Deploy em cloud (Railway / Render / AWS)
- [ ] Tratamento de erros mais robusto

### Médio prazo
- [ ] Múltiplas fontes RAG (ANVISA, OpenFDA)
- [ ] Histórico de conversas por usuário
- [ ] Rate limiting

### Longo prazo
- [ ] OCR de receitas médicas (foto → texto)
- [ ] Vector database para RAG semântico
- [ ] Dashboard de auditoria
- [ ] Integração com sistemas de farmácia

