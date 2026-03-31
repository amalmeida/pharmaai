# 🟢 Guia: Configurar WhatsApp Business API para PharmaAI

## Pré-requisitos
- Conta no Facebook (pessoal mesmo)
- Número de telefone (para verificação — **NÃO** use o número do seu WhatsApp pessoal)

---

## PASSO 1 — Criar conta Meta Developer

1. Acesse: **https://developers.facebook.com**
2. Clique em **"Começar"** / **"Get Started"**
3. Login com conta Facebook
4. Aceite os termos

---

## PASSO 2 — Criar um App

1. Clique em **"Criar App"** / **"Create App"**
2. Tipo: **"Business"**
3. Nome: `PharmaAI`
4. E-mail: seu e-mail
5. Criar

---

## PASSO 3 — Adicionar produto WhatsApp

1. No painel → **"Adicionar Produtos"**
2. Busque **"WhatsApp"** → **"Configurar"**

---

## PASSO 4 — Pegar credenciais (Sandbox)

No painel WhatsApp → **"API Setup"**:

- **Temporary access token** → copie (expira 24h)
- **Phone number ID** → copie (ex: `123456789012345`)
- Em **"To"** → adicione seu número de WhatsApp pessoal
- Clique **"Send Message"** para testar

---

## PASSO 5 — Configurar Webhook

### 5a. Expor app local com ngrok
```bash
ngrok http 8080
```
Copie a URL: `https://abc123.ngrok-free.app`

### 5b. No Meta → WhatsApp → Configuration
- **Callback URL**: `https://SEU-NGROK/webhook`
- **Verify token**: `pharmaai-verify-2024`
- Clique **"Verify and Save"**

### 5c. Webhook fields
- Marque **"messages"** ✅

---

## PASSO 6 — Configurar PharmaAI

`application.properties`:
```properties
whatsapp.api.token=SEU_TOKEN
whatsapp.api.phone-number-id=SEU_PHONE_NUMBER_ID
whatsapp.api.verify-token=pharmaai-verify-2024
openai.api.key=SUA_CHAVE_OPENAI
```

Ou variáveis de ambiente:
```
WHATSAPP_TOKEN=xxx
WHATSAPP_PHONE_NUMBER_ID=xxx
OPENAI_API_KEY=xxx
```

---

## PASSO 7 — Testar

1. `./mvnw spring-boot:run`
2. `ngrok http 8080`
3. Envie mensagem no WhatsApp para o número de teste
4. Aguarde resposta

---

## Links

- https://developers.facebook.com
- https://developers.facebook.com/docs/whatsapp/cloud-api
- https://ngrok.com/download
