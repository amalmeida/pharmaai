# 💊 PharmaAI — Contexto Atual e Status do Projeto

> **Atualizado em:** 2026-03-31

---

## 🎯 Objetivo do Sistema

Aplicação em **Java (Spring Boot + Arquitetura Hexagonal)** para validação de prescrições médicas e suporte farmacêutico usando **IA Generativa (LLM + RAG)**.

- Receber perguntas como: *"Posso tomar 5g de paracetamol?"*
- Buscar informações confiáveis de medicamentos (RAG)
- Enviar contexto + pergunta para LLM
- Retornar resposta estruturada (JSON com risco, alertas, recomendação)
- Integração via **WhatsApp Business API**

---

## 🏗️ Arquitetura Atual

```
domain/
  └── model/       → DrugInfo, LLMResponse
  └── port/out/    → LLMPort, WhatsAppPort

application/
  └── parser/      → LLMResponseParser
  └── prompt/      → PromptBuilderV2
  └── rag/         → DrugKnowledgeService, DrugNameExtractor,
                     DrugNameNormalizer, ContextFormatter,
                     DrugInfoProvider
  └── usecase/     → WhatsAppMessageProcessor (@Async)

adapter/
  └── in/web/      → WhatsAppWebhookController (webhook POST/GET)
  └── out/llm/     → OpenAILLMAdapter
  └── out/whatsapp/→ WhatsAppAdapter
  └── rag/         → DailyMedClient, DailyMedMapper,
                     DrugCacheRepository, HybridDrugInfoProvider

config/
  └── AppConfig, OpenAIConfig, WhatsAppConfig
```

---

## 🔄 Fluxo Completo

```
[Usuário WhatsApp] → [Meta Cloud API] → [POST /webhook]
    → WhatsAppWebhookController (retorna 200 imediato)
    → Processamento async:
        1. DrugNameExtractor (match rápido no cache)
        2. DrugNameNormalizer (LLM resolve nome se necessário)
        3. DailyMedClient (busca contexto RAG)
        4. DailyMedMapper (extrai seções do XML HL7 SPL)
        5. PromptBuilderV2 (monta prompt com contexto + regras)
        6. OpenAILLMAdapter (chama GPT-4o-mini)
        7. LLMResponseParser (converte resposta JSON)
    → WhatsAppAdapter (envia resposta formatada)
    → [Meta Cloud API] → [Usuário WhatsApp]
```

---

## ✅ O que já foi implementado

### RAG (Retrieval-Augmented Generation)
- ✔ `DailyMedClient` — Busca medicamentos na API DailyMed
- ✔ `DailyMedMapper` — Parse XML HL7 SPL (seções aninhadas)
- ✔ `DrugCacheRepository` — Cache local em JSON
- ✔ `HybridDrugInfoProvider` — Cache + DailyMed com fallback inteligente
- ✔ `DrugNameNormalizer` — LLM resolve nomes (tilenol→acetaminophen)
- ✔ `DrugNameExtractor` — Match rápido local (custo zero)
- ✔ Sinônimos PT-BR → inglês/US no DailyMedClient
- ✔ Múltiplos setIds com fallback (testa até encontrar conteúdo)

### LLM
- ✔ `OpenAILLMAdapter` — Integração com OpenAI (gpt-4o-mini)
- ✔ `PromptBuilderV2` — Prompts com contexto + regras adulto/infantil
- ✔ `LLMResponseParser` — Parse JSON estruturado

### WhatsApp
- ✔ `WhatsAppWebhookController` — Webhook GET (verificação) + POST (receber mensagens)
- ✔ `WhatsAppAdapter` — Enviar mensagens via Meta Cloud API
- ✔ `WhatsAppConfig` — Configuração externalizada (token, phoneId, verifyToken)
- ✔ Processamento assíncrono (@Async)
- ✔ Deduplicação de mensagens
- ✔ Formatação de resposta (risco com emoji, alertas, recomendação)

---

## 🔧 Problemas Resolvidos

| # | Problema | Solução |
|---|---|---|
| 1 | Erro 415 no DailyMed | Endpoint `.json` não existe para SPL individual, usar `.xml` |
| 2 | Erro 406 no DailyMed | Header `Accept: */*` (rejeita `application/xml`) |
| 3 | SPL sem texto (produtos internacionais) | Fallback por múltiplos setIds |
| 4 | "paracetamol" não encontrado | Mapa de sinônimos PT-BR → US (acetaminophen) |
| 5 | "tilenol sinos" não encontrado | `DrugNameNormalizer` via LLM |
| 6 | Sem info de idade do paciente | PromptBuilderV2 traz info adulto + infantil |

---

## 📍 Status Atual: Integração WhatsApp

**Código:** ✅ 100% implementado

**Configuração Meta:** ⬜ Pendente

### Próximos passos (ver `guia_passo_a_passo_whatsapp_meta.md`):

1. ✅ Criar conta Meta Developer → https://developers.facebook.com
2. ✅ Criar App "PharmaAI" (tipo Business) + Portfólio Empresarial
3. ✅ WhatsApp já adicionado (auto-configurado pelo Meta via "Casos de uso")
4. ✅ Token + Phone Number ID (979436765263547) configurados
5. ✅ Número pessoal verificado no sandbox (+55 51 99874 5556)
6. ✅ Teste de envio via API — mensagem entregue com sucesso! (template hello_world)
7. ✅ ngrok instalado e rodando (https://willis-nonretractile-geminally.ngrok-free.dev)
8. ✅ Webhook configurado e verificado no Meta
9. ✅ Campo "messages" marcado no webhook
10. ✅ Variáveis configuradas no application.properties
11. ✅ Teste E2E: WhatsApp → PharmaAI → WhatsApp ✅ **FUNCIONANDO!**

### 🎉 Resultado do Teste E2E (31/03/2026 11:56)
- **Pergunta:** "Posso tomar 5g de paracetamol?"
- **RAG:** Contexto de acetaminophen obtido do DailyMed (2524 chars)
- **LLM:** Respondeu em ~6s com risco HIGH
- **WhatsApp:** Resposta enviada com sucesso
- **Tempo total:** ~8 segundos

### 🧹 Limpeza de Código (31/03/2026)
Removidos arquivos não utilizados:
- `ProcessMessageService.java` → substituído por `WhatsAppMessageProcessor`
- `PromptBuilder.java` → substituído por `PromptBuilderV2`
- `PrescriptionParser.java` + `Prescription.java` → não utilizados
- `DrugKnowledgeBase.java` → substituído pelo RAG (DailyMed)
- `LocalJsonDrugInfoProvider.java` → substituído por `HybridDrugInfoProvider`
- `adapter/OpenAILLMAdapter.java` (duplicado) → mantido em `adapter/out/llm/`
- `src/application.yml` → fora do classpath, não utilizado

---

## 🛠️ Ferramentas Meta Developer

| Ferramenta | Link | Uso no PharmaAI |
|---|---|---|
| **Graph API Explorer** | https://developers.facebook.com/tools/explorer/ | Testar envio de mensagens manualmente |
| **Token Debugger** | https://developers.facebook.com/tools/debug/accesstoken/ | Verificar validade e permissões do token |
| **Sharing Debugger** | https://developers.facebook.com/tools/debug/sharing/ | Não relevante para este projeto |

---

## 📂 Documentação do Projeto

| Documento | Descrição |
|---|---|
| `pharma_ai_documento_de_contexto_e_roadmap.md` | Visão geral, conceitos, roadmap |
| `pharma_ai_contexto_problema_atual.md` | Status técnico detalhado (este arquivo) |
| `guia_whatsapp_business_api.md` | Guia resumido de configuração |
| `guia_passo_a_passo_whatsapp_meta.md` | **Guia completo passo a passo** com ferramentas Meta |

---

## 🚀 Roadmap

### Curto prazo (agora)
- [ ] Configurar WhatsApp Business API no Meta
- [ ] Teste E2E completo via WhatsApp

### Médio prazo
- [ ] Token permanente (System User)
- [ ] Deploy em cloud (Railway / Render / AWS)
- [ ] Múltiplas fontes RAG (ANVISA, OpenFDA)
- [ ] Histórico de conversas por usuário

### Longo prazo
- [ ] OCR de receitas médicas (foto → texto)
- [ ] Vector database para RAG semântico
- [ ] Dashboard de auditoria
- [ ] Integração com sistemas de farmácia
