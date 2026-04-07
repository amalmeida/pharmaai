# 💊 PharmaAI

Assistente farmacêutico inteligente via WhatsApp, com validação de prescrições usando **IA Generativa (LLM)** e **RAG** (Retrieval-Augmented Generation) com dados confiáveis do DailyMed/FDA (ANVISA dos EUA).

> **Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Arquitetura Hexagonal · GPT-4o-mini · WhatsApp Cloud API · DailyMed API

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
                                              │
                                              ▼
                                   8. QueryLogAdapter
                                      (salva auditoria no PostgreSQL)
```

### Integrações

| Serviço | Função | Endpoint |
|---------|--------|----------|
| **OpenAI GPT-4o-mini** | LLM para respostas e normalização de nomes | `api.openai.com/v1/chat/completions` |
| **DailyMed (NIH/FDA)** | Fonte confiável de dados de medicamentos (RAG) | `dailymed.nlm.nih.gov/dailymed/services/v2` |
| **WhatsApp Cloud API** | Receber e enviar mensagens via WhatsApp | `graph.facebook.com/v22.0` |
| **PostgreSQL 16** | Persistência de medicamentos + auditoria de consultas | `localhost:5432/pharmaai` |
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
│       ├── WhatsAppPort.java       #    Contrato: enviar mensagem WhatsApp
│       └── QueryLogPort.java       #    Contrato: salvar log de auditoria
│
├── application/                    # 🟢 Casos de uso — orquestração
│   ├── usecase/
│   │   └── WhatsAppMessageProcessor.java   # Processamento async + audit log
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
│   ├── out/persistence/                    # 🗄️ Banco de dados (JPA)
│   │   ├── DrugEntity.java                 # Entidade JPA: medicamentos
│   │   ├── QueryLogEntity.java             # Entidade JPA: auditoria de consultas
│   │   ├── DrugJpaRepository.java          # Repository Spring Data
│   │   ├── QueryLogJpaRepository.java      # Repository Spring Data
│   │   ├── DrugEntityMapper.java           # Mapper: DrugEntity ↔ DrugInfo
│   │   └── QueryLogAdapter.java            # Implementa QueryLogPort → banco
│   └── rag/
│       ├── DailyMedClient.java             # Busca dados no DailyMed (XML SPL)
│       ├── DailyMedMapper.java             # Extrai seções do XML HL7 SPL
│       └── HybridDrugInfoProvider.java     # DB → DailyMed fallback → auto-save
│
└── config/                         # ⚙️ Configuração Spring
    ├── AppConfig.java              #    Beans, @EnableAsync
    ├── DataMigrationRunner.java    #    Migra drugs-cache.json → banco na startup
    ├── OpenAIConfig.java           #    Props da OpenAI
    └── WhatsAppConfig.java         #    Props do WhatsApp
```

---

## 📦 Pré-requisitos

| Ferramenta | Versão | Link |
|------------|--------|------|
| **Java JDK** | 21+ | [Adoptium](https://adoptium.net/) |
| **Maven** | 3.9+ (incluso via `mvnw`) | — |
| **Docker Desktop** | qualquer | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **ngrok** | qualquer | [ngrok.com](https://ngrok.com/download) |
| **Conta Meta for Developers** | — | [developers.facebook.com](https://developers.facebook.com/) |
| **Chave OpenAI** | — | [platform.openai.com](https://platform.openai.com/api-keys) |

---

## 🚀 Setup rápido (passo a passo)

### 1️⃣ Clonar o projeto
```bash
git clone https://github.com/amalmeida/pharmaai.git
cd pharmaai
```

### 2️⃣ Subir o PostgreSQL (Docker)
```bash
docker compose up -d
```
> Isso cria o banco `pharmaai` com user `pharmaai` / senha `pharmaai123` na porta **5432**.
>
> Se já tem um Postgres rodando na 5432 (outro projeto), crie o banco manualmente:
> ```bash
> docker exec <nome_container_postgres> psql -U <usuario> -d <banco_existente> -c "CREATE DATABASE pharmaai;"
> docker exec <nome_container_postgres> psql -U <usuario> -d <banco_existente> -c "CREATE USER pharmaai WITH PASSWORD 'pharmaai123'; GRANT ALL PRIVILEGES ON DATABASE pharmaai TO pharmaai; ALTER DATABASE pharmaai OWNER TO pharmaai;"
> ```

### 3️⃣ Configurar tokens (ver seção [Configuração](#%EF%B8%8F-configuração))

### 4️⃣ Compilar o projeto

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd clean compile
```

**Linux/Mac:**
```bash
./mvnw clean compile
```

### 5️⃣ Iniciar a aplicação

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY = "sk-proj-SUA_CHAVE"
$env:WHATSAPP_TOKEN = "SEU_TOKEN_META"
.\mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY=sk-proj-SUA_CHAVE
export WHATSAPP_TOKEN=SEU_TOKEN_META
./mvnw spring-boot:run
```

> Na primeira execução, o sistema migra automaticamente os dados de `drugs-cache.json` para o PostgreSQL.

### 6️⃣ Iniciar o ngrok (em outro terminal)
```bash
ngrok http 8080
```

### 7️⃣ Configurar webhook no Meta com a URL do ngrok

### 8️⃣ Enviar mensagem pelo WhatsApp para o número de teste

---

## ⚙️ Configuração

Arquivo: `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/pharmaai}
    username: ${DATABASE_USER:pharmaai}
    password: ${DATABASE_PASSWORD:pharmaai123}
  jpa:
    hibernate:
      ddl-auto: update     # Cria/atualiza tabelas automaticamente

openai:
  api:
    key: ${OPENAI_API_KEY}
    model: gpt-4o-mini

whatsapp:
  api:
    token: ${WHATSAPP_TOKEN}
    phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID:979436765263547}
    verify-token: ${WHATSAPP_VERIFY_TOKEN:pharmaai-verify-2024}
```

> ⚠️ **Tokens temporários do Meta expiram em ~24h.** Veja [como renovar](#-renovando-o-token-do-whatsapp).

Recomendado: usar variáveis de ambiente em vez de hardcode:

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY = "sk-proj-..."
$env:WHATSAPP_TOKEN = "EAAw..."
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY=sk-proj-...
export WHATSAPP_TOKEN=EAAw...
```

---

## ▶️ Rodando a aplicação

### Windows (PowerShell)
```powershell
# 0. Garantir JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# 1. Subir banco (se não estiver rodando)
docker compose up -d

# 2. Configurar tokens
$env:OPENAI_API_KEY = "sk-proj-SUA_CHAVE"
$env:WHATSAPP_TOKEN = "SEU_TOKEN_META"

# 3. Compilar
.\mvnw.cmd clean compile

# 4. Iniciar a aplicação
.\mvnw.cmd spring-boot:run
```

### Linux/Mac
```bash
# 1. Subir banco
docker compose up -d

# 2. Configurar tokens
export OPENAI_API_KEY=sk-proj-SUA_CHAVE
export WHATSAPP_TOKEN=SEU_TOKEN_META

# 3. Compilar
./mvnw clean compile

# 4. Iniciar a aplicação
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080**. Verifique:
```
http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=pharmaai-verify-2024&hub.challenge=ok
```
Deve retornar: `ok`

### Verificar o banco
```bash
# Ver tabelas criadas
docker exec postgres psql -U pharmaai -d pharmaai -c "\dt"

# Ver medicamentos no banco
docker exec postgres psql -U pharmaai -d pharmaai -c "SELECT id, name, created_at FROM drugs;"

# Ver logs de auditoria
docker exec postgres psql -U pharmaai -d pharmaai -c "SELECT id, user_question, drug_found, risk_level, created_at FROM query_logs ORDER BY created_at DESC LIMIT 10;"
```

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
6. Atualize a variável de ambiente e reinicie:
   ```powershell
   $env:WHATSAPP_TOKEN = "NOVO_TOKEN_AQUI"
   .\mvnw.cmd spring-boot:run
   ```

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
| 🌐 | DB MISS → buscando no DailyMed |
| ✅ | Sucesso |
| 💾 | Salvo no banco de dados |
| 📚 | Contexto RAG obtido |
| 🤖 | LLM respondeu |
| 📱 | Número BR normalizado |
| 📤 | Enviando mensagem ao WhatsApp |
| 📊 | Query log salvo (auditoria) |
| ❌ | Erro (ver detalhes ao lado) |
| ⏭️ | Item ignorado (duplicata, tipo não-texto, etc.) |

---

## ❗ Problemas comuns

### "Port 8080 already in use"
→ Outra instância da app está rodando. Matar o processo:
```powershell
# Windows
Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### "Port 5432 already in use" (docker compose up)
→ Já tem um PostgreSQL rodando. Use o existente e crie o banco manualmente (ver [Setup rápido](#-setup-rápido-passo-a-passo)).

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

### Número BR com formato errado
→ O WhatsApp webhook envia o número sem o dígito 9 do celular (`555198745556`).
O `WhatsAppAdapter` corrige automaticamente para `5551998745556`.

---

## 📄 Checklist diário (abrir o projeto de manhã)

```
□ 1. Abrir terminal no diretório do projeto
□ 2. Garantir banco:     docker compose up -d
□ 3. Configurar JAVA:    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
□ 4. Configurar tokens:  $env:OPENAI_API_KEY = "..."; $env:WHATSAPP_TOKEN = "..."
□ 5. Compilar:           .\mvnw.cmd clean compile
□ 6. Iniciar app:        .\mvnw.cmd spring-boot:run
□ 7. Iniciar ngrok:      ngrok http 8080  (outro terminal)
□ 8. Copiar URL ngrok:   http://localhost:4040
□ 9. Atualizar webhook no Meta (se URL mudou)
□ 10. Renovar token WhatsApp (se expirou)
□ 11. Testar:            enviar msg no WhatsApp
□ 12. Verificar logs:    Get-Content target/pharmaai.log -Wait -Tail 20
```

---

## 📝 Licença

Projeto acadêmico / estudo — não para uso comercial em produção.

