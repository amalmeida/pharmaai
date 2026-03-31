package com.pharmaai.application.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.List;

/**
 * Extrator rápido de nomes de medicamentos.
 * Faz match direto com a lista de drogas conhecidas (do cache).
 * Se não encontrar, o DrugKnowledgeService usa o DrugNameNormalizer (LLM) como fallback.
 */
public class DrugNameExtractor {

    private static final Logger log = LoggerFactory.getLogger(DrugNameExtractor.class);

    private final List<String> knownDrugs;

    public DrugNameExtractor(List<String> knownDrugs) {
        this.knownDrugs = knownDrugs;
    }

    /**
     * Tenta extrair o nome do medicamento do input por match direto com knownDrugs.
     * Retorna null se não encontrar (fallback para LLM no DrugKnowledgeService).
     */
    public String extract(String input) {

        String normalizedInput = normalize(input);

        for (String drug : knownDrugs) {
            if (normalizedInput.contains(normalize(drug))) {
                log.info("⚡ Match rápido (cache): {}", drug);
                return drug;
            }
        }


        return null;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }
}