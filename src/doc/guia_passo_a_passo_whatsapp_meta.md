# 📱 PharmaAI — Guia Passo a Passo: WhatsApp Business API + Meta Developer Tools

> **Data:** 2026-03-30
> **Status:** Código pronto (Controller + Adapter + Config). Falta configuração no Meta.

---

## 📋 Resumo do que já temos no código

| Componente | Arquivo | Status |
|---|---|---|
| Webhook (GET verificação) | `WhatsAppWebhookController.java` | ✅ Pronto |
| Webhook (POST receber msg) | `WhatsAppWebhookController.java` | ✅ Pronto |
| Enviar resposta (adapter) | `WhatsAppAdapter.java` | ✅ Pronto |
| Port (interface) | `WhatsAppPort.java` | ✅ Pronto |
| Config (token, phoneId) | `WhatsAppConfig.java` | ✅ Pronto |
| Properties | `application.properties` | ✅ Pronto |

---

## 🚀 PASSOS PARA CONFIGURAR NO META

### PASSO 1 — Criar Conta de Desenvolvedor Meta

1. Acesse: **https://developers.facebook.com**
2. Clique em **"Começar"** (ou "Get Started")
3. Faça login com sua conta Facebook pessoal
4. Aceite os termos de desenvolvedor
5. Complete a verificação (e-mail/telefone)

> ⚠️ Você precisa de uma conta Facebook ativa. Pode ser sua conta pessoal.

---

### PASSO 2 — Criar um App

1. No dashboard, clique em **"Criar App"** / **"Create App"**
2. Selecione o tipo: **"Business"** (ou "Outro" → "Business")
3. Preencha:
   - **Nome do App**: `PharmaAI`
   - **E-mail de contato**: seu e-mail
   - **Business Account**: pode pular (criar depois)
4. Clique **"Criar App"**

---

### PASSO 3 — Configurar WhatsApp (Caso de Uso)

> ⚠️ A interface do Meta mudou. O WhatsApp agora é configurado automaticamente como "Caso de uso".

1. No **Painel** do app, clique em:
   **"Personalizar o caso de uso: Conectar-se com os clientes pelo WhatsApp"**
2. Ou no menu lateral → **"Casos de uso"**
3. Siga as instruções para configurar a API do WhatsApp

---

### PASSO 4 — Obter Credenciais (Sandbox/Teste)

No menu lateral → **WhatsApp** → **"API Setup"** / **"Configuração da API"**

Aqui você encontra **3 informações essenciais**:

#### 4a. Temporary Access Token (Token Temporário)
- Clique **"Generate"** para gerar
- ⚠️ **Expira em 24h** — para produção, use token permanente
- Copie e salve

#### 4b. Phone Number ID
- Visível na seção "From" / "De"
- Ex: `123456789012345`
- Copie e salve

#### 4c. Número de teste (From)
- O Meta fornece um número sandbox para testes
- Ex: `+1 555 XXX XXXX`

---

### PASSO 5 — Adicionar Número Destinatário (Para Testes)

1. Na seção **"To"** / **"Para"**, clique **"Manage phone number list"**
2. Adicione **seu número pessoal de WhatsApp** (com DDI)
   - Ex: `+5511999998888`
3. Você receberá um **código de verificação no WhatsApp**
4. Insira o código para confirmar

> ⚠️ No modo sandbox, você SÓ pode enviar para números verificados aqui.

---

### PASSO 6 — Testar Envio Manual (Graph API Explorer)

Aqui é onde entra o **Explorador da Graph API** que você mencionou:

1. Acesse: **https://developers.facebook.com/tools/explorer/**
2. Configure:
   - **App**: selecione `PharmaAI`
   - **Token**: cole o token temporário do Passo 4a
3. Método: **POST**
4. URL: `{PHONE_NUMBER_ID}/messages`
5. Body (JSON):

```json
{
  "messaging_product": "whatsapp",
  "to": "5511999998888",
  "type": "text",
  "text": {
    "body": "Olá! PharmaAI conectado com sucesso! 💊"
  }
}
```

6. Clique **"Submit"** / **"Enviar"**
7. ✅ Você deve receber a mensagem no seu WhatsApp!

> 💡 Use o Graph API Explorer para testar chamadas ANTES de conectar com seu app Java.

---

### PASSO 7 — Configurar Webhook (Receber mensagens)

#### 7a. Expor sua aplicação local com ngrok

```bash
# Terminal 1: Rodar o PharmaAI
cd C:\workspace\pharmaai
.\mvnw.cmd spring-boot:run

# Terminal 2: Expor com ngrok
ngrok http 8080
```

O ngrok vai gerar uma URL pública, ex:
```
https://abc123.ngrok-free.app
```

#### 7b. Configurar no Meta

1. No painel WhatsApp → **"Configuration"** / **"Configuração"**
2. Na seção **"Webhook"**, clique **"Edit"** / **"Editar"**
3. Preencha:
   - **Callback URL**: `https://abc123.ngrok-free.app/webhook`
   - **Verify Token**: `pharmaai-verify-2024`
4. Clique **"Verify and Save"** / **"Verificar e Salvar"**

> O Meta vai fazer um **GET** no seu endpoint com `hub.verify_token`.
> Nosso `WhatsAppWebhookController` já responde corretamente ✅

#### 7c. Inscrever-se nos campos do Webhook

Após verificar, marque os campos:
- ✅ **messages** (obrigatório — receber mensagens)
- ⬜ message_template_status_updates (opcional)
- ⬜ account_alerts (opcional)

---

### PASSO 8 — Configurar Variáveis no PharmaAI

#### Opção A: Variáveis de Ambiente (Recomendado)

```powershell
# PowerShell
$env:WHATSAPP_TOKEN = "EAAxxxxxxx..."
$env:WHATSAPP_PHONE_NUMBER_ID = "123456789012345"
$env:WHATSAPP_VERIFY_TOKEN = "pharmaai-verify-2024"
$env:OPENAI_API_KEY = "sk-xxxxxxx..."
```

#### Opção B: Direto no application.properties (Apenas dev local)

```properties
whatsapp.api.token=EAAxxxxxxx...
whatsapp.api.phone-number-id=123456789012345
whatsapp.api.verify-token=pharmaai-verify-2024
openai.api.key=sk-xxxxxxx...
```

---

### PASSO 9 — Testar o Fluxo Completo

1. Certifique-se de que o app está rodando (`mvnw spring-boot:run`)
2. Certifique-se de que o ngrok está ativo
3. Abra o WhatsApp no seu celular
4. Envie uma mensagem para o **número de teste do Meta**:

```
Posso tomar 5g de paracetamol?
```

5. Observe os logs do Spring Boot:

```
📩 WhatsApp de 5511999998888: Posso tomar 5g de paracetamol?
🔄 Processando mensagem de 5511999998888...
📚 Contexto obtido
🤖 LLM respondeu
📤 WhatsApp mensagem enviada para: 5511999998888 | Status: 200 OK
✅ Resposta enviada para 5511999998888
```

6. ✅ Receba a resposta formatada no WhatsApp!

---

## 🔧 Ferramentas de Desenvolvedor Meta — Para que servem

### 1. Explorador da Graph API
🔗 https://developers.facebook.com/tools/explorer/

**Para que usar:**
- Testar envio de mensagens manualmente
- Debugar chamadas à API antes de codificar
- Verificar se seu token está funcionando
- Testar endpoints sem precisar do app Java

**Como usar no PharmaAI:**
```
POST /{PHONE_NUMBER_ID}/messages
Header: Authorization: Bearer {TOKEN}
Body: { "messaging_product": "whatsapp", "to": "...", "text": { "body": "..." } }
```

---

### 2. Depurador de Token de Acesso
🔗 https://developers.facebook.com/tools/debug/accesstoken/

**Para que usar:**
- Verificar se o token está válido
- Ver quando expira
- Ver quais permissões o token tem
- Diagnosticar erro 401/403

**Quando usar no PharmaAI:**
- Se o `WhatsAppAdapter` retornar erro de autenticação
- Se o token temporário expirou (24h)
- Para verificar permissões do token permanente

---

### 3. Depurador de Compartilhamento
🔗 https://developers.facebook.com/tools/debug/sharing/

**Para que usar:**
- Testar preview de links compartilhados no Facebook/WhatsApp
- **Menos relevante para PharmaAI** (é mais para sites e conteúdo)

---

## ⚠️ Problemas Comuns

| Problema | Causa | Solução |
|---|---|---|
| Webhook não verifica | Token errado | Conferir `whatsapp.api.verify-token` |
| Webhook não verifica | App não está rodando | Verificar `mvnw spring-boot:run` + ngrok |
| Mensagem não chega | Campo "messages" não marcado | Ativar no webhook fields |
| Erro 401 ao enviar | Token expirado | Gerar novo token no Meta |
| Mensagem não entrega | Número não verificado | Adicionar número na lista sandbox |
| Erro 400 ao enviar | Formato do body errado | Verificar JSON no Graph API Explorer |
| Erro na resposta | Timeout LLM | Aumentar timeout do RestTemplate |

---

## 📊 Sequência do Fluxo Completo

```
[Usuário WhatsApp]
       │
       ▼ (mensagem)
[Meta Cloud API]
       │
       ▼ (POST /webhook)
[ngrok] ──► [Spring Boot: WhatsAppWebhookController]
                    │
                    ├── Retorna 200 OK imediatamente
                    │
                    ▼ (async)
              [DrugKnowledgeService] → RAG (Cache + DailyMed)
                    │
                    ▼
              [PromptBuilderV2] → Monta prompt com contexto
                    │
                    ▼
              [OpenAILLMAdapter] → Chama GPT-4o-mini
                    │
                    ▼
              [LLMResponseParser] → Parse JSON
                    │
                    ▼
              [WhatsAppAdapter] → POST para Meta Cloud API
                    │
                    ▼
[Meta Cloud API] → Entrega mensagem ao usuário
```

---

## 🔐 Produção (Futuro)

Para ir para produção, será necessário:

1. **Verificação do Business** no Meta Business Suite
2. **Número de telefone próprio** (não o sandbox)
3. **Token permanente** (System User Token):
   - Business Settings → System Users → Generate Token
4. **Aprovação do App** pelo Meta (review)
5. **Templates de mensagem** aprovados (para iniciar conversas)
6. **HTTPS com certificado válido** (não ngrok)

---

## ✅ Checklist Rápido

- [ ] Conta Meta Developer criada
- [ ] App `PharmaAI` criado
- [ ] Produto WhatsApp adicionado
- [ ] Token temporário gerado
- [ ] Phone Number ID copiado
- [ ] Número pessoal verificado no sandbox
- [ ] Teste manual via Graph API Explorer ✅
- [ ] ngrok instalado e rodando
- [ ] Spring Boot rodando (port 8080)
- [ ] Webhook configurado e verificado
- [ ] Campo "messages" marcado no webhook
- [ ] Variáveis configuradas no `application.properties`
- [ ] Teste E2E: enviar msg no WhatsApp → receber resposta ✅

---

## 📎 Links Úteis

- Meta Developers: https://developers.facebook.com
- WhatsApp Cloud API Docs: https://developers.facebook.com/docs/whatsapp/cloud-api
- Graph API Explorer: https://developers.facebook.com/tools/explorer/
- Token Debugger: https://developers.facebook.com/tools/debug/accesstoken/
- ngrok: https://ngrok.com/download
- WhatsApp Business Pricing: https://developers.facebook.com/docs/whatsapp/pricing

