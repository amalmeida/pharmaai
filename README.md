# 💊 PharmaAI

Assistente farmacêutico inteligente via WhatsApp, com validação de prescrições usando **IA Generativa (LLM)** e **RAG** com dados do DailyMed/FDA.

> **Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Arquitetura Hexagonal · GPT-4o-mini · WhatsApp Cloud API · DailyMed API

---

## 🚀 Setup Local

### Pré-requisitos

- Java JDK 21+ ([Adoptium](https://adoptium.net/))
- Docker Desktop
- ngrok ([ngrok.com](https://ngrok.com/download))
- Conta [Meta for Developers](https://developers.facebook.com/)
- Chave [OpenAI](https://platform.openai.com/api-keys)

### 1. Clonar e subir o banco

```bash
git clone https://github.com/amalmeida/pharmaai.git
cd pharmaai
docker compose up -d
```

### 2. Configurar tokens

Crie o arquivo `src/main/resources/application-local.yml`:

```yaml
openai:
  api:
    key: SUA_CHAVE_OPENAI

whatsapp:
  api:
    token: SEU_TOKEN_WHATSAPP
```

> ⚠️ Este arquivo está no `.gitignore` — nunca será commitado.

### 3. Compilar e rodar

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd spring-boot:run -DskipTests "-Dspring-boot.run.profiles=local"
```

### 4. Verificar

```
http://localhost:8080/actuator/health  → {"status":"UP"}
```

---

## 🌐 ngrok + Webhook Meta

### 1. Iniciar ngrok

```bash
ngrok http 8080
```

Copie a URL pública (ex: `https://xxxx.ngrok-free.dev`).

> 💡 Para URL fixa (grátis): crie um domínio em [dashboard.ngrok.com/domains](https://dashboard.ngrok.com/domains) e use `ngrok http 8080 --url=SEU_DOMINIO`

### 2. Configurar webhook no Meta

1. [developers.facebook.com](https://developers.facebook.com/) → App **PharmaAI**
2. Menu: **Conectar no WhatsApp** → **Configuração**
3. Webhook:
   - **URL de callback:** `https://xxxx.ngrok-free.dev/webhook`
   - **Verificar token:** `pharmaai-verify-2024`
   - Clique em **Verificar e salvar**
4. Em **Campos do webhook**, ative **messages** (Assinar)

### 3. Testar

Envie uma mensagem para o número de teste do WhatsApp e aguarde ~10s.

---

## 📊 Comandos úteis

```powershell
# Acompanhar logs em tempo real
Get-Content target/pharmaai.log -Wait -Tail 20

# Ver medicamentos no banco
docker exec postgres psql -U pharmaai -d pharmaai -c "SELECT id, name FROM drugs;"

# Ver últimas consultas
docker exec postgres psql -U pharmaai -d pharmaai -c "SELECT user_question, risk_level, created_at FROM query_logs ORDER BY created_at DESC LIMIT 5;"

# Liberar porta 8080 (se ocupada)
Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

---

## 📁 Estrutura

```
src/main/java/com/pharmaai/
├── domain/          # Modelos e contratos (ports)
├── application/     # Casos de uso, RAG, prompt, parser
├── adapter/         # WhatsApp, OpenAI, DailyMed, PostgreSQL
└── config/          # Configurações Spring
```

> 📖 Documentação completa: [`src/doc/pharma_ai_documento_de_contexto_e_roadmap.md`](src/doc/pharma_ai_documento_de_contexto_e_roadmap.md)
