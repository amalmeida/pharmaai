package com.pharmaai.application.rag;

import com.pharmaai.domain.port.out.LLMPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usa a LLM para normalizar nomes de medicamentos antes da busca no DailyMed.
 *
 * Exemplos:
 *   "tilenol sinos"     → "acetaminophen"
 *   "paracetamol"       → "acetaminophen"
 *   "novalgina"         → "dipyrone"
 *   "advil"             → "ibuprofen"
 *
 * A LLM recebe um prompt curto e retorna APENAS o nome do princípio ativo em inglês.
 * Isso elimina a necessidade de mapas estáticos de sinônimos/aliases.
 */
public class DrugNameNormalizer {

    private static final Logger log = LoggerFactory.getLogger(DrugNameNormalizer.class);

    private final LLMPort llmPort;

    private static final String PROMPT_TEMPLATE = """
            You are a pharmaceutical name resolver. Given a drug-related text from a user, extract the MAIN active ingredient name in English (as used in the US FDA/DailyMed database).
            
            Rules:
            - Return ONLY the active ingredient name in English, nothing else
            - If it's a brand name, return the active ingredient (e.g. "Tylenol" → "acetaminophen")
            - If it's misspelled, correct it (e.g. "tilenol sinos" → "acetaminophen")
            - If it's in another language, translate to the US pharmacological name (e.g. "paracetamol" → "acetaminophen")
            - If the text contains multiple drugs, return only the main/first one
            - If no drug is found, return "UNKNOWN"
            - Response must be a single word or compound name, lowercase, no punctuation
            
            User text: "%s"
            """;

    public DrugNameNormalizer(LLMPort llmPort) {
        this.llmPort = llmPort;
    }

    /**
     * Normaliza o texto do usuário para o nome do princípio ativo em inglês.
     * Retorna null se não conseguir identificar o medicamento.
     */
    public String normalize(String userInput) {
        try {
            String prompt = PROMPT_TEMPLATE.formatted(userInput);
            String response = llmPort.ask(prompt).trim().toLowerCase();

            // Limpar possíveis artefatos da LLM
            response = response
                    .replaceAll("[\"'`.!,;:\\-]", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (response.isBlank() || response.contains("unknown") || response.length() > 100) {
                log.warn("⚠️ LLM não identificou medicamento no input");
                return null;
            }

            log.info("🧠 LLM normalizou: \"{}\" → \"{}\"", userInput, response);
            return response;

        } catch (Exception e) {
            log.error("❌ Erro ao normalizar nome via LLM: {}", e.getMessage(), e);
            return null;
        }
    }
}
