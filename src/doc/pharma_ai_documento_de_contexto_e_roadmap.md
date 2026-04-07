# 💊 PharmaAI — Documento de Contexto e Roadmap

> Última atualização: Abril 2026

## 🎯 Visão Geral do Projeto

O **PharmaAI** é um assistente farmacêutico inteligente via WhatsApp, usando **IA Generativa (LLM + RAG)** para validar prescrições e responder dúvidas sobre medicamentos com base em dados confiáveis.

### Objetivos

- Auxiliar farmacêuticos na validação de prescrições
- Responder dúvidas sobre medicamentos com dados reais (DailyMed/FDA)
- Garantir respostas seguras, estruturadas e auditáveis
- Disponibilizar para uso real por profissionais de saúde

---

## 🧠 Conceitos-Chave

| Conceito | Descrição |
|----------|-----------|
| **IA Generativa** | Geração de conteúdo (texto) por modelos de linguagem |
| **LLM** | Large Language Model — `gpt-4o-mini` usado no projeto |
| **RAG** | Retrieval-Augmented Generation — busca dados reais antes de gerar resposta |
| **Alucinação** | Quando a LLM gera informação plausível mas incorreta — RAG reduz isso |

---

## 🏗️ Arquitetura

**Arquitetura Hexagonal (Ports & Adapters)**

| Camada | Responsabilidade |
|--------|-----------------|
| `domain/` | Modelos e contratos (ports) — SEM dependência de framework |
| `application/` | Casos de uso, RAG service, PromptBuilder, parser |
| `adapter/` | Integrações: OpenAI, DailyMed, WhatsApp, Cache |
| `config/` | Configurações Spring |

### Fluxo Completo

```
WhatsApp → Meta → ngrok → Webhook → [async] →
  DrugNameExtractor → DrugNameNormalizer (LLM) →
  DrugKnowledgeService (Cache + DailyMed XML) →
  PromptBuilderV2 (contexto RAG) →
  OpenAI GPT-4o-mini → LLMResponseParser →
  WhatsAppAdapter → Meta → WhatsApp ✅
```

---

## ✅ O que já foi implementado (Fase 1 + 2 + 3)

### ✔ Integração LLM
- [x] Adapter OpenAI (GPT-4o-mini)
- [x] Configuração externa (apiKey, model, url)
- [x] PromptBuilder v2 com contexto RAG e regras de segurança
- [x] Parser JSON → `LLMResponse` (answer, risk_level, alerts, recommendation)

### ✔ RAG — Retrieval-Augmented Generation
- [x] DailyMedClient — busca medicamentos via API DailyMed (XML SPL)
- [x] DailyMedMapper — parseia XML HL7 SPL (indicações, dosagem, warnings, contraindicações, efeitos)
- [x] DrugCacheRepository — cache local (`drugs-cache.json`)
- [x] HybridDrugInfoProvider — cache local → fallback DailyMed
- [x] DrugNameExtractor — match rápido por nomes conhecidos
- [x] DrugNameNormalizer — LLM resolve nomes comerciais BR → nome internacional (ex: "tilenol" → "acetaminophen")
- [x] ContextFormatter — formata `DrugInfo` → texto para prompt

### ✔ WhatsApp (Meta Cloud API)
- [x] Webhook GET (verificação do Meta)
- [x] Webhook POST (recebe mensagens)
- [x] Processamento assíncrono (@Async)
- [x] Deduplicação de mensagens
- [x] WhatsAppAdapter — envia resposta formatada ao usuário
- [x] Normalização de número BR (5551 → 55519)
- [x] Formatação rica (emojis, negrito, listas)

### ✔ Infra
- [x] ngrok para expor localhost ao Meta
- [x] Logs estruturados com emojis (`target/pharmaai.log`)
- [x] `.gitignore` protegendo tokens e arquivos sensíveis
- [x] Repositório Git: https://github.com/amalmeida/pharmaai.git

---

## 📚 Fontes de Dados (RAG)

| Fonte | Status | Uso |
|-------|--------|-----|
| **DailyMed (NIH/FDA)** | ✅ Integrado | XML SPL — bulas oficiais dos EUA |
| **Cache local (JSON)** | ✅ Integrado | Evita chamadas repetidas à API |
| **ANVISA** | 🔜 Futuro | Bulas oficiais do Brasil |
| **WHO (OMS)** | 🔜 Futuro | Protocolos globais |

---

## ⚠️ Segurança e Responsabilidade

O sistema SEMPRE deve:

- ❌ **Não** fornecer diagnóstico
- ❌ **Não** prescrever medicamentos
- ✅ Recomendar consulta profissional
- ✅ Sinalizar riscos e alertas
- ✅ Informar fonte dos dados
- ✅ Incluir disclaimer em todas as respostas

---

# 🚀 Roadmap — O que falta para Produção

## Fase 4: Banco de Dados (próximo passo)

> Sair do cache JSON e ter persistência real.

### 🤔 Qual banco usar? Comparativo

| Banco | Tipo | Prós | Contras | Veredicto |
|-------|------|------|---------|-----------|
| **PostgreSQL** | Relacional | Robusto, Spring Data JPA nativo, suporta JSON, **pgvector** para busca semântica futura | Schema rígido (mas OK para nosso caso) | ✅ **Recomendado** |
| **MongoDB** | Documento | Flexível para bulas (documentos grandes), schema-free | Mais complexo para relacionamentos, menos suporte Spring nativo que JPA | ⚠️ Boa opção, mas Postgres resolve |
| **Redis** | Cache/KV | Ultra-rápido, ótimo para cache | Não é banco principal, dados em memória, perde ao reiniciar (sem persistência padrão) | ⚠️ Complementar, não substitui DB |

### 💡 Recomendação: **PostgreSQL**

**Por quê:**
1. **Spring Data JPA** funciona perfeitamente — já estamos no ecossistema Spring
2. **pgvector** — extensão que adiciona busca vetorial (semântica) ao Postgres, eliminando necessidade de banco separado para vector search no futuro
3. **JSON columns** — Postgres suporta `jsonb`, podemos guardar a bula crua como JSON se precisar
4. **Redis como complemento** — no futuro, usar Redis para cache de consultas frequentes (substituindo o `drugs-cache.json`)

### Tarefas

- [ ] **Adicionar dependências** (Spring Data JPA + PostgreSQL driver)
- [ ] **Criar entidades JPA**
  - `DrugEntity` — dados do medicamento (nome, indicação, dosagem, warnings, etc.)
  - `QueryLogEntity` — histórico de consultas (quem perguntou, quando, resposta)
  - `UserEntity` — farmacêuticos cadastrados (futuro)
- [ ] **Migrar cache JSON → banco**
  - Script de importação `drugs-cache.json` → tabela `drugs`
- [ ] **Criar repositórios Spring Data**
  - `DrugRepository extends JpaRepository<DrugEntity, Long>`
  - `QueryLogRepository` — para auditoria
- [ ] **Atualizar HybridDrugInfoProvider**
  - Prioridade: DB → DailyMed API → resposta sem contexto
  - Salvar automaticamente no DB após busca no DailyMed
- [ ] **Docker Compose** para PostgreSQL
  - Atualizar `compose.yaml` com serviço `postgres`

---

## Fase 5: Token Permanente do WhatsApp

> Token temporário expira em ~24h. Para produção, precisamos de um token permanente.

- [ ] **Criar System User no Meta Business Manager**
  1. Acessar [business.facebook.com](https://business.facebook.com/)
  2. Configurações → Usuários → Usuários do sistema → Adicionar
  3. Criar usuário do sistema com papel "Admin"
- [ ] **Gerar token permanente (System User Token)**
  1. No System User criado → Gerar token
  2. Selecionar o app PharmaAI
  3. Permissões: `whatsapp_business_messaging`, `whatsapp_business_management`
  4. Gerar → token **não expira**
- [ ] **Vincular WhatsApp Business Account ao System User**
  1. Configurações → Contas → Contas do WhatsApp
  2. Adicionar pessoas → selecionar System User → acesso total
- [ ] **Atualizar application.properties** com token permanente
- [ ] **Mover tokens para variáveis de ambiente** (segurança)
  - `OPENAI_API_KEY`, `WHATSAPP_TOKEN` via env vars ou secret manager
- [ ] **Registrar número de telefone real** (sair do sandbox)
  1. Meta → WhatsApp → Começar → Adicionar número de telefone
  2. Verificar com SMS/ligação
  3. Definir nome de exibição e foto do perfil

---

## Fase 6: Deploy no Google Cloud (FREE TIER) 🌩️

> Sair do ngrok e ter a aplicação rodando na nuvem, com HTTPS automático e custo zero para teste.

### ❓ Por que HTTPS se é só WhatsApp?

O HTTPS **não é para os usuários** — é uma **exigência do Meta**. O webhook que recebe mensagens do WhatsApp **precisa ser HTTPS**. Hoje usamos ngrok para isso. Com o **Google Cloud Run**, ganhamos HTTPS automático e gratuito — sem precisar comprar domínio!

### 💡 Por que Google Cloud?

| Recurso | Free Tier | Uso no PharmaAI |
|---------|-----------|-----------------|
| **Cloud Run** | 2M requests/mês, 360K GB-s memória, 180K vCPU-s | App Spring Boot (webhook + LLM) |
| **Cloud SQL (Postgres)** | $300 créditos (90 dias) | Banco de dados |
| **Secret Manager** | 6 secrets ativos grátis | API keys, tokens |
| **Cloud Build** | 120 min/dia grátis | CI/CD automático |
| **Artifact Registry** | 500MB grátis | Imagens Docker |

**Vantagens:**
- ✅ **HTTPS automático** — Cloud Run gera URL `https://pharmaai-xxxxx-uc.a.run.app`
- ✅ **Sem domínio** — basta usar a URL do Cloud Run como webhook no Meta
- ✅ **Sem ngrok** — elimina dependência local
- ✅ **Escala a zero** — não cobra quando ninguém usa
- ✅ **$300 créditos grátis** — suficiente para meses de teste
- ✅ **Experiência real com GCP** — diferencial no currículo

### Tarefas — Deploy

- [ ] **Criar conta Google Cloud** (se ainda não tem)
  1. Acessar [cloud.google.com](https://cloud.google.com/)
  2. Ativar free trial ($300 créditos por 90 dias)
  3. Criar projeto: `pharmaai`
- [ ] **Instalar Google Cloud CLI (gcloud)**
  ```
  # Windows - baixar installer:
  # https://cloud.google.com/sdk/docs/install
  gcloud init
  gcloud auth login
  gcloud config set project pharmaai
  ```
- [ ] **Criar Dockerfile**
  ```dockerfile
  FROM eclipse-temurin:21-jre-alpine
  COPY target/pharmaai-0.0.1-SNAPSHOT.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- [ ] **Build e push da imagem**
  ```bash
  # Compilar o JAR
  ./mvnw clean package -DskipTests
  # Build e deploy direto no Cloud Run
  gcloud run deploy pharmaai \
    --source . \
    --region southamerica-east1 \
    --allow-unauthenticated \
    --set-env-vars "OPENAI_API_KEY=xxx,WHATSAPP_TOKEN=xxx,WHATSAPP_VERIFY_TOKEN=xxx"
  ```
- [ ] **Cloud SQL (PostgreSQL) — quando Fase 4 estiver pronta**
  ```bash
  gcloud sql instances create pharmaai-db \
    --database-version POSTGRES_15 \
    --tier db-f1-micro \
    --region southamerica-east1
  ```
- [ ] **Secret Manager para tokens**
  ```bash
  echo -n "sk-xxx" | gcloud secrets create openai-api-key --data-file=-
  echo -n "EAAx..." | gcloud secrets create whatsapp-token --data-file=-
  ```
- [ ] **Atualizar webhook no Meta**
  - Trocar URL do ngrok pela URL do Cloud Run
  - URL será algo como: `https://pharmaai-xxxxx-uc.a.run.app/webhook`

### Tarefas — Monitoramento e Segurança

- [ ] **Monitoramento e logs**
  - Cloud Run já integra com **Cloud Logging** (logs automáticos)
  - Health check endpoint (`/actuator/health`)
  - Alertas via **Cloud Monitoring** (email gratuito)
- [ ] **Rate limiting e segurança**
  - Limitar requisições por usuário/minuto
  - Validar origem dos webhooks (assinatura do Meta)
  - Proteger endpoints administrativos
- [ ] **Onboarding de farmacêuticos**
  - Número WhatsApp Business dedicado com nome "PharmaAI"
  - Mensagem de boas-vindas automática
  - Instruções de uso (ex: "Envie sua dúvida sobre medicamentos")
  - Disclaimer legal em toda interação
- [ ] **Testes automatizados**
  - Testes unitários (domain, application)
  - Testes de integração (adapters)
  - Teste E2E simulando webhook completo

---

## Fase 7: Evolução Avançada (futuro)

- [ ] **Busca semântica com pgvector (no próprio PostgreSQL)**
  - Instalar extensão `pgvector` no Postgres
  - Gerar embeddings das bulas via OpenAI Embeddings API
  - Buscar medicamentos por similaridade semântica (não só por nome exato)
  - RAG avançado com ranking de relevância
  - ⚠️ Se escalar muito → migrar para Pinecone, Weaviate ou Qdrant (vector DB dedicado)
- [ ] **Redis para cache de consultas frequentes**
  - Substituir `drugs-cache.json` por Redis
  - TTL configurável por medicamento
  - Cache de respostas LLM para perguntas repetidas
- [ ] **Múltiplas fontes RAG**
  - ANVISA (bulas brasileiras)
  - WHO/OMS
  - Bases proprietárias de farmácias
- [ ] **OCR de receitas médicas**
  - Usuário envia foto da receita
  - Sistema extrai medicamentos via OCR
  - Valida dosagens e interações
- [ ] **Histórico de consultas por usuário**
  - Farmacêutico vê histórico de perguntas
  - Auditoria completa
- [ ] **Painel web administrativo**
  - Dashboard com métricas de uso
  - Gerenciar medicamentos no banco
  - Ver logs de consultas
- [ ] **Interações medicamentosas**
  - Detectar conflitos entre múltiplos medicamentos
  - Alertar sobre combinações perigosas
- [ ] **Multi-idioma**
  - Respostas em PT-BR e EN
  - Normalização de nomes em ambos idiomas

---

# 📊 Resumo do Status

| Fase | Descrição | Status |
|------|-----------|--------|
| **Fase 1** | Integração LLM + PromptBuilder | ✅ Concluído |
| **Fase 2** | RAG com DailyMed + Cache | ✅ Concluído |
| **Fase 3** | WhatsApp (webhook + envio de resposta) | ✅ Concluído |
| **Fase 4** | Banco de dados (PostgreSQL) | 🔜 Próximo |
| **Fase 5** | Token permanente do WhatsApp | 🔜 Próximo |
| **Fase 6** | Deploy Google Cloud (Cloud Run) + onboarding | 📋 Planejado |
| **Fase 7** | Vector DB, OCR, painel admin | 📋 Futuro |

---

# 🧠 Insight Final

O valor do PharmaAI **não está apenas no LLM**.

Está na **orquestração completa**:

- 🔍 Busca de dados confiáveis (RAG)
- 🧠 Normalização inteligente de nomes
- 📋 Resposta estruturada com risco e alertas
- 📱 Entrega via WhatsApp (canal real do farmacêutico)
- 🔒 Segurança e disclaimers em toda interação
- 📊 Auditoria e rastreabilidade

👉 **Isso transforma um chatbot em um produto real para farmacêuticos.**

