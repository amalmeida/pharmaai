# 💊 PharmaAI — Documento de Contexto e Roadmap

## 🎯 Visão Geral do Projeto

O **PharmaAI** é um projeto de estudo e prototipação de uma solução baseada em **IA Generativa (LLMs)** com foco na área farmacêutica.

Objetivo principal:

- Auxiliar farmacêuticos na validação de prescrições
- Responder dúvidas sobre medicamentos
- Garantir respostas seguras e com responsabilidade
- Evoluir para um sistema confiável com uso de **RAG (Retrieval-Augmented Generation)**

---

# 🧠 Conceitos Fundamentais

## IA Generativa

IA Generativa é um tipo de inteligência artificial capaz de **gerar conteúdo novo**, como texto, código ou imagens.

No nosso caso:
👉 Geração de respostas sobre medicamentos

---

## LLM (Large Language Model)

Modelos treinados com grandes volumes de texto que conseguem:

- Entender linguagem natural
- Gerar respostas coerentes
- Extrair informações de texto

Exemplo usado no projeto:
- `gpt-4o-mini`

---

## ⚠️ Limitação Importante

LLMs podem:

- Gerar respostas incorretas (alucinação)
- Parecer confiáveis mesmo quando estão errados

👉 Por isso, precisamos de validação e RAG

---

# 🏗️ Arquitetura do Projeto

## Estilo

- Arquitetura Hexagonal (Ports & Adapters)
- Separação clara de responsabilidades

## Camadas

### Domain
- Regras de negócio
- Portas (interfaces)

### Application
- Use cases
- PromptBuilder

### Adapter
- Integração com LLM
- Futuro: WhatsApp, OCR

---

## 🔄 Fluxo Atual

```
Usuário → PromptBuilder → LLM → Resposta
```

---

# ✅ O que já foi implementado

## ✔ Integração com LLM

- Adapter para OpenAI
- Uso de API real
- Configuração externa (apiKey, model)

## ✔ PromptBuilder v1

- Regras de segurança
- Controle de resposta
- Estrutura padronizada

## ✔ Teste funcional

Exemplo:

Pergunta:
```
Para que serve dipirona?
```

Resposta:
```
Explicação correta + alerta de segurança
```

---

# 🧠 Próxima evolução: RAG

## O que é RAG?

RAG (Retrieval-Augmented Generation) combina:

1. Busca de informação confiável
2. Uso do LLM para gerar resposta baseada nessa informação

---

## 🔄 Fluxo com RAG

```
Pergunta
   ↓
Busca em base confiável
   ↓
Contexto
   ↓
LLM gera resposta baseada no contexto
```

---

## 🎯 Benefícios

- Reduz alucinação
- Aumenta confiabilidade
- Permite auditoria
- Controla fonte da informação

---

# 📚 Fontes confiáveis para medicamentos

## 🌎 Internacionais

### DailyMed
https://dailymed.nlm.nih.gov/dailymed/

- Bulas oficiais
- Dados estruturados
- Excelente para integração futura

---

### FDA
https://www.fda.gov/drugs

- Aprovação de medicamentos
- Documentação técnica

---

### WHO (OMS)
https://www.who.int/medicines

- Protocolos globais
- Diretrizes de uso

---

## 🇧🇷 Brasil

### ANVISA
https://consultas.anvisa.gov.br/#/bulario/

- Bulas oficiais no Brasil
- Importante para contexto local

---

# 🧠 Estratégia de uso dessas fontes

## Fase 1 (Atual)
- LLM sem RAG
- Prompt controlado

## Fase 2 (Próxima)
- Buscar dados dessas fontes
- Inserir no prompt como contexto

Exemplo:

```
Contexto:
- Dose máxima: X mg

Pergunta:
...
```

---

## Fase 3 (Avançado)

- Indexação dos dados
- Busca semântica (vector database)
- RAG completo

---

# ⚠️ Segurança e responsabilidade

O sistema deve SEMPRE:

- Não fornecer diagnóstico
- Não prescrever medicamentos
- Recomendar consulta profissional
- Sinalizar riscos

---

# 🚀 Próximos passos

## Curto prazo

- Melhorar PromptBuilder (v2)
- Criar parser de prescrição
- Estruturar resposta em JSON

## Médio prazo

- Implementar RAG simples
- Integrar com base de medicamentos

## Longo prazo

- Integração com WhatsApp
- OCR de receitas
- Histórico e auditoria

---

# 🧠 Insight Final

O valor do sistema NÃO está apenas no LLM.

Está em:

- Orquestração
- Validação
- Fontes confiáveis
- Segurança

👉 Isso transforma um chatbot em um produto real.

