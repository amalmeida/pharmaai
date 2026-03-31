# 💊 PharmaAI

Assistente farmacêutico inteligente via WhatsApp, com validação de prescrições usando **IA Generativa (LLM)** e **RAG** (Retrieval-Augmented Generation) com dados confiáveis do DailyMed/FDA (ANVISA dos EUA).

> **Stack:** Java 21 · Spring Boot 3.3 · Arquitetura Hexagonal · GPT-4o-mini · WhatsApp Cloud API · DailyMed API

---

## 📋 Índice

- [Como funciona](#-como-funciona)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Setup rápido (passo a passo)](#-setup-rápido-passo-a-passo)
- [Configuração](#%EF%B8%8F-configuração)
- [Rodando a aplicação](#-rodando-a-aplicação)
- [Subindo o ngrok](#-subindo-o-ngrok)
- [Configurando o Webhook no Meta](#-configurando-o-webhook-no-meta)
- [Renovando o token do WhatsApp](#-renovando-o-token-do-whatsapp)
- [Testando](#-testando)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Logs e debug](#-logs-e-debug)
- [Problemas comuns](#-problemas-comuns)

---

## 🧠 Como funciona

O usuário envia uma pergunta pelo WhatsApp (ex: *"Posso tomar 5g de paracetamol?"*) e recebe uma resposta estruturada com risco, alertas e recomendação.

### Fluxo completo

```
[Usuário WhatsApp]
    │ envia mensagem
    ▼
[Meta Cloud API] ──POST──▶ [ngrok] ──▶ [localhost:8080/webhook]
                                              │
                                              ▼
                                   [WhatsAppWebhookController]
                                       retorna 200 imediato
                                              │ @Async
                                              ▼
                                   [WhatsAppMessageProcessor]
                                              │
                     ┌────────────────────────┼────────────────────────┐
                     ▼                        ▼                        ▼
            1. DrugNameExtractor     2. DrugNameNormalizer     3. DailyMedClient
               (match local)            (LLM resolve nome)       (busca RAG)
                     │                        │                        │
                     └────────────────────────┼────────────────────────┘
                                              ▼
                                   4. PromptBuilderV2
                                      (monta prompt com contexto)
                                              │
                                              ▼
                                   5. OpenAI GPT-4o-mini
                                      (gera resposta JSON)
                                              │
                                              ▼
                                   6. LLMResponseParser
                                      (parse JSON → LLMResponse)
                                              │
                                              ▼
                                   7. WhatsAppAdapter
                                      (envia resposta formatada)
                                              │
                                              ▼
                                   [Meta Cloud API] ──▶ [Usuário WhatsApp] ✅
```

### Integrações

| Serviço | Função | Endpoint |
|---------|--------|----------|
| **OpenAI GPT-4o-mini** | LLM para respostas e normalização de nomes | `api.openai.com/v1/chat/completions` |
| **DailyMed (NIH/FDA)** | Fonte confiável de dados de medicamentos (RAG) | `dailymed.nlm.nih.gov/dailymed/services/v2` |
| **WhatsApp Cloud API** | Receber e enviar mensagens via WhatsApp | `graph.facebook.com/v22.0` |
| **ngrok** | Túnel público para expor localhost ao Meta | `localhost:4040` (painel) |

### Exemplo de resposta no WhatsApp

```
💊 *Resposta:*
A dose máxima recomendada de paracetamol para adultos é de 4.000 mg (4g)
em 24 horas. Tomar 5g excede essa dose e pode causar danos severos ao fígado.

🔴 *Risco:* HIGH

⚠️ *Alertas:*
• Overdose de paracetamol pode causar insuficiência hepática
• Não exceder 4g em 24 horas

👨‍⚕️ *Recomendação:*
Não tome 5g. Consulte um médico ou farmacêutico para orientação.

_PharmaAI — Sempre consulte um profissional de saúde._
```

---

## 🏗 Arquitetura

Arquitetura Hexagonal (Ports & Adapters):

```
src/main/java/com/pharmaai/
│
├── domain/                         # 🔵 Núcleo — sem dependências externas
│   ├── model/
│   │   ├── DrugInfo.java           #    Dados do medicamento (nome, dose, efeitos)
│   │   └── LLMResponse.java        #    Resposta estruturada da LLM
│   └── port/out/
│       ├── LLMPort.java            #    Contrato: chamar LLM
│       └── WhatsAppPort.java       #    Contrato: enviar mensagem WhatsApp
│
├── application/                    # 🟢 Casos de uso — orquestração
│   ├── usecase/
│   │   └── WhatsAppMessageProcessor.java   # Processamento async da mensagem
│   ├── rag/
│   │   ├── DrugKnowledgeService.java       # Orquestra busca de contexto
│   │   ├── DrugNameExtractor.java          # Match rápido por nome conhecido
│   │   ├── DrugNameNormalizer.java         # LLM normaliza nome (ex: "tilenol" → "acetaminophen")
│   │   ├── DrugInfoProvider.java           # Interface do provider
│   │   └── ContextFormatter.java           # Formata DrugInfo → texto para prompt
│   ├── prompt/
│   │   └── PromptBuilderV2.java            # Monta prompt com contexto RAG + regras
│   └── parser/
│       └── LLMResponseParser.java          # Parse JSON → LLMResponse
│
├── adapter/                        # 🟠 Integrações externas
│   ├── in/web/
│   │   └── WhatsAppWebhookController.java  # Recebe webhooks do Meta (GET + POST)
│   ├── out/llm/
│   │   └── OpenAILLMAdapter.java           # Implementa LLMPort → OpenAI API
│   ├── out/whatsapp/
│   │   └── WhatsAppAdapter.java            # Implementa WhatsAppPort → Meta API
│   └── rag/
│       ├── DailyMedClient.java             # Busca dados no DailyMed (XML SPL)
│       ├── DailyMedMapper.java             # Extrai seções do XML HL7 SPL
│       ├── DrugCacheRepository.java        # Cache local (drugs-cache.json)
│       └── HybridDrugInfoProvider.java     # Cache + DailyMed fallback
│
└── config/                         # ⚙️ Configuração Spring
    ├── AppConfig.java              #    Beans, @EnableAsync
    ├── OpenAIConfig.java           #    Props da OpenAI
    └── WhatsAppConfig.java         #    Props do WhatsApp
```

---

## 📦 Pré-requisitos

| Ferramenta | Versão | Link |
|------------|--------|------|
| **Java JDK** | 21+ | [Adoptium](https://adoptium.net/) |
| **Maven** | 3.9+ (incluso via `mvnw`) | — |
| **ngrok** | qualquer | [ngrok.com](https://ngrok.com/download) |
| **Conta Meta for Developers** | — | [developers.facebook.com](https://developers.facebook.com/) |
| **Chave OpenAI** | — | [platform.openai.com](https://platform.openai.com/api-keys) |

---

## 🚀 Setup rápido (passo a passo)

### 1️⃣ Clonar e compilar
```bash
git clone <repo-url>
cd pharmaai
./mvnw compile
```

### 2️⃣ Configurar tokens (ver seção [Configuração](#%EF%B8%8F-configuração))

### 3️⃣ Iniciar a aplicação
```bash
./mvnw spring-boot:run
```

### 4️⃣ Iniciar o ngrok (em outro terminal)
```bash
ngrok http 8080
```

### 5️⃣ Configurar webhook no Meta com a URL do ngrok

### 6️⃣ Enviar mensagem pelo WhatsApp para o número de teste

---

## ⚙️ Configuração

Arquivo: `src/main/resources/application.properties`

```properties
# OpenAI
openai.api.key=${OPENAI_API_KEY:<SUA_CHAVE_OPENAI>}
openai.api.url=https://api.openai.com/v1/chat/completions
openai.api.model=gpt-4o-mini

# WhatsApp Business API (Meta Cloud API)
whatsapp.api.token=${WHATSAPP_TOKEN:<SEU_TOKEN_META>}
whatsapp.api.phone-number-id=${WHATSAPP_PHONE_NUMBER_ID:979436765263547}
whatsapp.api.verify-token=${WHATSAPP_VERIFY_TOKEN:pharmaai-verify-2024}
whatsapp.api.url=https://graph.facebook.com/v22.0
```

> ⚠️ **Tokens temporários do Meta expiram em ~24h.** Veja [como renovar](#-renovando-o-token-do-whatsapp).

Você pode usar variáveis de ambiente em vez de hardcode:
```bash
export OPENAI_API_KEY=sk-proj-...
export WHATSAPP_TOKEN=EAAw...
```

---

## ▶️ Rodando a aplicação

### Windows (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
cd C:\workspace\pharmaai
.\mvnw.cmd spring-boot:run
```

### Linux/Mac
```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080**. Verifique:
```
http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=pharmaai-verify-2024&hub.challenge=ok
```
Deve retornar: `ok`

---

## 🌐 Subindo o ngrok

O ngrok cria um túnel público para o Meta acessar seu localhost.

### 1. Iniciar (em outro terminal)
```bash
ngrok http 8080
```

### 2. Copiar a URL pública
O ngrok mostra algo como:
```
Forwarding   https://a1b2c3d4.ngrok-free.app -> http://localhost:8080
```

Copie a URL `https://xxxx.ngrok-free.app`.

### 3. Ver a URL (se perdeu)
Acesse o painel do ngrok: **http://localhost:4040**

Ou via API:
```powershell
(Invoke-RestMethod http://localhost:4040/api/tunnels).tunnels[0].public_url
```

> ⚠️ **Toda vez que reiniciar o ngrok, a URL muda!** Precisa atualizar no Meta.

---

## 📱 Configurando o Webhook no Meta

Toda vez que a URL do ngrok mudar, atualize no Meta:

1. Acesse [developers.facebook.com](https://developers.facebook.com/)
2. Selecione o app **PharmaAI**
3. Menu lateral: **WhatsApp** → **Configuration**
4. Na seção **Webhook**:
   - **Callback URL:** `https://xxxx.ngrok-free.app/webhook`
   - **Verify Token:** `pharmaai-verify-2024`
   - Clique em **Verify and Save**
5. Em **Webhook fields**, certifique-se que **messages** está marcado (Subscribe)

---

## 🔑 Renovando o token do WhatsApp

Os tokens temporários do Meta **expiram em ~24h**. Para renovar:

1. Acesse [developers.facebook.com](https://developers.facebook.com/)
2. Selecione o app **PharmaAI**
3. Menu lateral: **WhatsApp** → **API Setup**
4. Na seção **Temporary access token**, clique em **Generate new token**
5. Copie o token gerado
6. Atualize em `application.properties`:
   ```properties
   whatsapp.api.token=${WHATSAPP_TOKEN:<NOVO_TOKEN>}
   ```
7. Reinicie a aplicação

### Testar se o token funciona (PowerShell)
```powershell
$token = "<SEU_TOKEN>"
$body = '{"messaging_product":"whatsapp","to":"5551998745556","type":"text","text":{"body":"teste"}}'
$headers = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
Invoke-WebRequest -Uri "https://graph.facebook.com/v22.0/979436765263547/messages" `
  -Method Post -Headers $headers -Body $body -UseBasicParsing
```
- **200** → token OK ✅
- **401** → token expirado ❌ → gerar novo

---

## 🧪 Testando

### Teste 1: Aplicação rodando?
```bash
curl http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=pharmaai-verify-2024&hub.challenge=test
# Deve retornar: test
```

### Teste 2: Simular webhook localmente (sem ngrok)
```powershell
$payload = '{"object":"whatsapp_business_account","entry":[{"id":"930451043030152","changes":[{"value":{"messaging_product":"whatsapp","metadata":{"display_phone_number":"15551749365","phone_number_id":"979436765263547"},"contacts":[{"profile":{"name":"Test"},"wa_id":"555198745556"}],"messages":[{"from":"555198745556","id":"wamid.TEST001","timestamp":"1711900000","text":{"body":"Posso tomar 5g de paracetamol?"},"type":"text"}]},"field":"messages"}]}]}'

Invoke-WebRequest -Uri "http://localhost:8080/webhook" -Method Post `
  -ContentType "application/json" -Body $payload -UseBasicParsing
```
Verifique os logs em `target/pharmaai.log`.

### Teste 3: Fluxo completo via WhatsApp
1. Garanta que a aplicação está rodando (`mvnw spring-boot:run`)
2. Garanta que o ngrok está rodando (`ngrok http 8080`)
3. Garanta que o webhook no Meta aponta para a URL do ngrok
4. Garanta que o token não expirou
5. Envie uma mensagem para o número de teste: **+1 (555) 174-9365**
6. Aguarde ~10s e verifique a resposta no WhatsApp

---

## 📊 Logs e debug

### Onde ficam os logs
```
target/pharmaai.log
```

### Acompanhar em tempo real (PowerShell)
```powershell
Get-Content target/pharmaai.log -Wait -Tail 20
```

### O que cada emoji significa nos logs

| Emoji | Significado |
|-------|-------------|
| 📨 | Webhook POST recebido |
| 📩 | Mensagem de texto extraída |
| 🚀 | Processamento async disparado |
| 🔄 | Processando mensagem |
| ⚡ | Match rápido no cache local |
| 🧠 | LLM consultada para normalizar nome |
| 🌐 | Cache MISS → buscando no DailyMed |
| ✅ | Sucesso |
| 📚 | Contexto RAG obtido |
| 🤖 | LLM respondeu |
| 📱 | Número BR normalizado |
| 📤 | Enviando mensagem ao WhatsApp |
| ❌ | Erro (ver detalhes ao lado) |
| ⏭️ | Item ignorado (duplicata, tipo não-texto, etc.) |

---

## ❗ Problemas comuns

### "Nenhum log aparece após enviar mensagem no WhatsApp"
→ O ngrok não está rodando ou a URL no Meta está desatualizada.
```bash
# Verificar se ngrok está ativo:
curl http://localhost:4040/api/tunnels
# Se erro → reiniciar: ngrok http 8080
# Depois atualizar URL no Meta → WhatsApp → Configuration → Webhook
```

### "WhatsApp API erro HTTP 401"
→ Token expirado. [Renovar token](#-renovando-o-token-do-whatsapp).

### "Recipient phone number not in allowed list (131030)"
→ No modo sandbox do Meta, só é possível enviar para números cadastrados.  
Vá em **WhatsApp → API Setup → To** e adicione o número de destino.

### "Port 8080 already in use"
→ Outra instância da app está rodando.
```powershell
# Encontrar e matar o processo:
netstat -ano | findstr ":8080"
taskkill /PID <PID> /F
```

### Número BR com formato errado
→ O WhatsApp webhook envia o número sem o dígito 9 do celular (`555198745556`).  
O `WhatsAppAdapter` corrige automaticamente para `5551998745556`.

---

## 📄 Checklist diário (abrir o projeto de manhã)

```
□ 1. Abrir terminal no diretório do projeto
□ 2. Compilar:          .\mvnw.cmd compile
□ 3. Iniciar app:       .\mvnw.cmd spring-boot:run
□ 4. Iniciar ngrok:     ngrok http 8080  (outro terminal)
□ 5. Copiar URL ngrok:  http://localhost:4040
□ 6. Atualizar webhook no Meta (se URL mudou)
□ 7. Renovar token WhatsApp (se expirou) → application.properties
□ 8. Testar:            enviar msg no WhatsApp
□ 9. Verificar logs:    target/pharmaai.log
```

---

## 📝 Licença

Projeto acadêmico / estudo — não para uso comercial em produção.

