package com.pharmaai.application.rag;

import com.pharmaai.domain.port.out.LLMPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DrugKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(DrugKnowledgeService.class);

    private final DrugInfoProvider provider;
    private final ContextFormatter formatter;
    private final DrugNameExtractor extractor;
    private final DrugNameNormalizer normalizer;

    public DrugKnowledgeService(DrugInfoProvider provider, List<String> knownDrugs, LLMPort llmPort) {
        this.provider = provider;
        this.formatter = new ContextFormatter();
        this.extractor = new DrugNameExtractor(knownDrugs);
        this.normalizer = new DrugNameNormalizer(llmPort);
    }

    public String getContext(String userInput) {

        // 1. Tenta extração rápida por nome conhecido (sem custo de LLM)
        String drugName = extractor.extract(userInput);

        // 2. Se não encontrou, usa LLM para normalizar o nome
        if (drugName == null) {
            log.info("🧠 Nome não reconhecido localmente, consultando LLM...");
            drugName = normalizer.normalize(userInput);
        }

        if (drugName == null) {
            return "Nenhuma informação confiável encontrada.";
        }

        return provider.findByName(drugName)
                .map(formatter::format)
                .orElse("Nenhuma informação confiável encontrada.");
    }
}