# 📋 PharmaAI — Contexto Completo e Guia de Setup

> Documento de referência para entender o projeto, o problema resolvido, e como rodar/testar.

---

## 1. O que é o PharmaAI

Aplicação em **Java (Spring Boot 3.3 + Arquitetura Hexagonal)** para validação de prescrições médicas e suporte farmacêutico via WhatsApp, usando **IA Generativa (LLM)** + **RAG** (Retrieval-Augmented Generation).

### Objetivo

- Receber perguntas como: *"Posso tomar 5g de paracetamol?"*
- Buscar informações confiáveis de medicamentos (RAG via DailyMed/FDA)
- Enviar contexto + pergunta para a LLM (GPT-4o-mini)
- Retornar resposta estruturada (JSON com risco, alertas, recomendação)
- Entregar a resposta formatada de volta no WhatsApp

---

## 2. Arquitetura Hexagonal

```
domain/        → regras e contratos (ports) — SEM dependência de framework
application/   → casos de uso (PromptBuilder, parser, RAG service)
adapter/       → integrações externas
  ├── in/web/          → WhatsApp Webhook (recebe mensagens)
  ├── out/llm/         → OpenAI (LLM)
  ├── out/whatsapp/    → Meta Cloud API (envia respostas)
  └── rag/             → DailyMed + Cache local
config/        → configurações Spring
```

### Princípio: Domain puro

- `domain/` não depende de Spring, Jackson ou qualquer biblioteca externa
- `LLMPort` e `WhatsAppPort` são interfaces (ports)
- Os adapters implementam essas interfaces

---

## 3. Fluxo Completo

```
1. Usuário envia mensagem no WhatsApp
2. Meta envia POST para ngrok → localhost:8080/webhook
3. WhatsAppWebhookController extrai a mensagem
4. WhatsAppMessageProcessor (async):
   a. DrugNameExtractor → tenta match rápido local
   b. DrugNameNormalizer → LLM normaliza nome (ex: "tilenol" → "acetaminophen")
   c. DrugKnowledgeService → busca contexto:
      - Cache local (drugs-cache.json)
      - Se cache MISS → DailyMed API (XML SPL)
   d. PromptBuilderV2 → monta prompt com contexto RAG
   e. OpenAI GPT-4o-mini → gera resposta JSON
   f. LLMResponseParser → parse JSON → LLMResponse
   g. WhatsAppAdapter → envia resposta formatada ao usuário
5. Usuário recebe resposta no WhatsApp ✅
```

---

## 4. Integrações

| Serviço | Função | Endpoint |
|---------|--------|----------|
| **OpenAI GPT-4o-mini** | LLM para respostas e normalização de nomes | `api.openai.com/v1/chat/completions` |
| **DailyMed (NIH/FDA)** | Fonte confiável de medicamentos (RAG) — XML SPL | `dailymed.nlm.nih.gov/dailymed/services/v2` |
| **WhatsApp Cloud API** | Receber e enviar mensagens via WhatsApp | `graph.facebook.com/v22.0` |
| **ngrok** | Túnel público para expor localhost ao Meta | `localhost:4040` (painel) |

---

## 5. Problema Resolvido: DailyMed 415

### Problema

O endpoint `/spls/{setId}.json` do DailyMed retornava **415 Unsupported Media Type**.

### Causa

- O `RestTemplate.exchange()` enviava headers incompatíveis (Content-Type em GET)
- O endpoint `.json` do DailyMed não retorna dados completos de seções

### Solução aplicada

- Mudamos para buscar o **XML SPL** (`/spls/{setId}.xml`) via `restTemplate.getForObject()`
- Criamos o `DailyMedMapper` para parsear o XML HL7 SPL e extrair seções:
  - `INDICATIONS & USAGE SECTION`
  - `DOSAGE & ADMINISTRATION SECTION`
  - `WARNINGS SECTION`
  - `CONTRAINDICATIONS SECTION`
  - `ADVERSE REACTIONS SECTION`
- Mapeamos para o objeto `DrugInfo`

### Problema adicional resolvido: Normalização de nomes

- Usuário digita "tilenol sinos" → DailyMed não encontra
- Solução: `DrugNameNormalizer` usa a LLM para traduzir nomes comerciais/genéricos BR → nome internacional (ex: `acetaminophen`)

---

## 6. Como Rodar (Passo a Passo)

### Pré-requisitos

- **Java JDK 21+** → [Adoptium](https://adoptium.net/)
- **ngrok** → [ngrok.com](https://ngrok.com/download)
- **Conta Meta for Developers** → [developers.facebook.com](https://developers.facebook.com/)
- **Chave OpenAI** → [platform.openai.com](https://platform.openai.com/api-keys)

### 6.1 Clonar e compilar

```bash
git clone https://github.com/amalmeida/pharmaai.git
cd pharmaai
```

### 6.2 Configurar tokens

Editar `src/main/resources/application.properties`:

```properties
# OpenAI
openai.api.key=${OPENAI_API_KEY}          # ← variável de ambiente OU coloque a chave direto
openai.api.url=https://api.openai.com/v1/chat/completions
openai.api.model=gpt-4o-mini

# WhatsApp Business API (Meta Cloud API)
whatsapp.api.token=${WHATSAPP_TOKEN}      # ← variável de ambiente OU coloque o token direto
whatsapp.api.phone-number-id=${WHATSAPP_PHONE_NUMBER_ID:979436765263547}
whatsapp.api.verify-token=${WHATSAPP_VERIFY_TOKEN:pharmaai-verify-2024}
whatsapp.api.url=https://graph.facebook.com/v22.0
```

**Opção via variáveis de ambiente (recomendado):**

```powershell
# Windows PowerShell
$env:OPENAI_API_KEY = "sk-proj-..."
$env:WHATSAPP_TOKEN = "EAAw..."
```

```bash
# Linux/Mac
export OPENAI_API_KEY=sk-proj-...
export WHATSAPP_TOKEN=EAAw...
```

> ⚠️ **Tokens temporários do Meta expiram em ~24h!**

### 6.3 Compilar e iniciar a aplicação

```powershell
# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
cd C:\workspace\pharmaai

# 1. Compilar primeiro (verifica se tudo está OK)
.\mvnw.cmd clean compile

# 2. Iniciar a aplicação
.\mvnw.cmd spring-boot:run
```

```bash
# Linux/Mac

# 1. Compilar primeiro
./mvnw clean compile

# 2. Iniciar a aplicação
./mvnw spring-boot:run
```

Verificar se subiu:
```
http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=pharmaai-verify-2024&hub.challenge=ok
→ Deve retornar: ok
```

### 6.4 Iniciar o ngrok (outro terminal)

```bash
ngrok http 8080
```

Copiar a URL pública (ex: `https://a1b2c3d4.ngrok-free.app`).

> **Painel ngrok:** http://localhost:4040
>
> **Toda vez que reiniciar o ngrok, a URL muda!** Precisa atualizar no Meta.

### 6.5 Configurar webhook no Meta

1. Acesse [developers.facebook.com](https://developers.facebook.com/)
2. Selecione o app **PharmaAI**
3. Menu lateral: **WhatsApp** → **Configuration**
4. Seção **Webhook**:
   - **Callback URL:** `https://xxxx.ngrok-free.app/webhook`
   - **Verify Token:** `pharmaai-verify-2024`
   - Clique em **Verify and Save**
5. Em **Webhook fields**, marque **messages** (Subscribe)

### 6.6 Renovar token do WhatsApp (se expirou)

1. Acesse [developers.facebook.com](https://developers.facebook.com/)
2. App **PharmaAI** → **WhatsApp** → **API Setup**
3. Seção **Temporary access token** → **Generate new token**
4. Copie e atualize em `application.properties` ou variável de ambiente
5. Reinicie a aplicação

### 6.7 Testar

Envie uma mensagem para o número de teste no WhatsApp:
```
Posso tomar 5g de paracetamol?
```

Acompanhe os logs:
```powershell
Get-Content target/pharmaai.log -Wait -Tail 30
```

---

## 7. Checklist Diário (abrir o projeto de manhã)

```
□ 1. Abrir terminal no diretório do projeto
□ 2. Setar JAVA_HOME se necessário:  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
□ 3. Compilar:          .\mvnw.cmd compile
□ 4. Iniciar app:       .\mvnw.cmd spring-boot:run
□ 5. Iniciar ngrok:     ngrok http 8080         (outro terminal)
□ 6. Copiar URL ngrok:  http://localhost:4040
□ 7. Atualizar webhook no Meta (se URL mudou)
□ 8. Renovar token WhatsApp (se expirou) → application.properties → reiniciar app
□ 9. Testar:            enviar msg no WhatsApp
□ 10. Verificar logs:   Get-Content target/pharmaai.log -Wait -Tail 30
```

---

## 8. Teste local (sem WhatsApp)

### Simular webhook localmente

```powershell
$payload = '{"object":"whatsapp_business_account","entry":[{"id":"930451043030152","changes":[{"value":{"messaging_product":"whatsapp","metadata":{"display_phone_number":"15551749365","phone_number_id":"979436765263547"},"contacts":[{"profile":{"name":"Test"},"wa_id":"555198745556"}],"messages":[{"from":"555198745556","id":"wamid.TEST001","timestamp":"1711900000","text":{"body":"Posso tomar 5g de paracetamol?"},"type":"text"}]},"field":"messages"}]}]}'

Invoke-WebRequest -Uri "http://localhost:8080/webhook" -Method Post `
  -ContentType "application/json" -Body $payload -UseBasicParsing
```

### Testar token do WhatsApp

```powershell
$token = "<SEU_TOKEN>"
$body = '{"messaging_product":"whatsapp","to":"5551998745556","type":"text","text":{"body":"teste PharmaAI"}}'
$headers = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
Invoke-WebRequest -Uri "https://graph.facebook.com/v22.0/979436765263547/messages" `
  -Method Post -Headers $headers -Body $body -UseBasicParsing
```

- **200** → token OK ✅
- **401** → token expirado ❌ → gerar novo

---

## 9. Problemas Comuns

| Problema | Causa | Solução |
|----------|-------|---------|
| Nenhum log após enviar msg no WhatsApp | ngrok não está rodando ou URL desatualizada no Meta | Reiniciar ngrok, atualizar URL no Meta |
| HTTP 401 na API do WhatsApp | Token expirado (~24h) | Gerar novo token no Meta |
| "Recipient phone number not in allowed list" (131030) | Número não cadastrado no sandbox | Meta → WhatsApp → API Setup → adicionar número em "To" |
| Port 8080 already in use | Outra instância rodando | `netstat -ano \| findstr ":8080"` → `taskkill /PID <PID> /F` |
| 415 Unsupported Media Type (DailyMed) | Endpoint .json não funciona para detalhes | Já corrigido: usamos .xml |
| "release version 21 not supported" | JAVA_HOME apontando para JDK < 21 | `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` |

---

## 10. Próximos Passos (Roadmap)

- [ ] Token permanente do WhatsApp (System User Token)
- [ ] Melhorar RAG: ranking de relevância, múltiplas fontes
- [ ] Futuro: vector database para busca semântica
- [ ] Suporte a múltiplos idiomas
- [ ] Deploy em cloud (AWS/GCP)
- [ ] Testes automatizados (unitários + integração)

---

## 11. Estrutura de Arquivos

```
pharmaai/
├── src/
│   ├── main/java/com/pharmaai/
│   │   ├── PharmaaiApplication.java
│   │   ├── domain/
│   │   │   ├── model/DrugInfo.java, LLMResponse.java
│   │   │   └── port/out/LLMPort.java, WhatsAppPort.java
│   │   ├── application/
│   │   │   ├── usecase/WhatsAppMessageProcessor.java
│   │   │   ├── rag/DrugKnowledgeService.java, DrugNameExtractor.java, ...
│   │   │   ├── prompt/PromptBuilderV2.java
│   │   │   └── parser/LLMResponseParser.java
│   │   ├── adapter/
│   │   │   ├── in/web/WhatsAppWebhookController.java
│   │   │   ├── out/llm/OpenAILLMAdapter.java
│   │   │   ├── out/whatsapp/WhatsAppAdapter.java
│   │   │   └── rag/DailyMedClient.java, DailyMedMapper.java, ...
│   │   └── config/AppConfig.java, OpenAIConfig.java, WhatsAppConfig.java
│   └── main/resources/application.properties
├── drugs-cache.json
├── pom.xml
├── compose.yaml
├── .gitignore
└── README.md
```

---

*Última atualização: Abril 2026*

