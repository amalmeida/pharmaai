package com.pharmaai.config;

import com.pharmaai.adapter.rag.DailyMedClient;
import com.pharmaai.adapter.rag.DailyMedMapper;
import com.pharmaai.adapter.rag.DrugCacheRepository;
import com.pharmaai.adapter.rag.HybridDrugInfoProvider;
import com.pharmaai.application.parser.LLMResponseParser;
import com.pharmaai.application.prompt.PromptBuilderV2;
import com.pharmaai.application.rag.DrugInfoProvider;
import com.pharmaai.application.rag.DrugKnowledgeService;
import com.pharmaai.domain.port.out.LLMPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean
    public DrugCacheRepository drugCacheRepository() {
        return new DrugCacheRepository();
    }

    @Bean
    public DailyMedClient dailyMedClient() {
        return new DailyMedClient();
    }

    @Bean
    public DailyMedMapper dailyMedMapper() {
        return new DailyMedMapper();
    }

    @Bean
    public DrugInfoProvider drugInfoProvider(DrugCacheRepository cacheRepository,
                                             DailyMedClient client,
                                             DailyMedMapper mapper) {
        return new HybridDrugInfoProvider(cacheRepository, client, mapper);
    }

    @Bean
    public DrugKnowledgeService drugKnowledgeService(DrugInfoProvider provider,
                                                      DrugCacheRepository cacheRepository,
                                                      LLMPort llmPort) {
        List<String> knownDrugs = cacheRepository.load()
                .stream()
                .map(d -> d.getName())
                .toList();

        if (knownDrugs.isEmpty()) {
            knownDrugs = List.of("paracetamol", "dipirona", "ibuprofeno");
        }

        return new DrugKnowledgeService(provider, knownDrugs, llmPort);
    }

    @Bean
    public PromptBuilderV2 promptBuilderV2() {
        return new PromptBuilderV2();
    }

    @Bean
    public LLMResponseParser llmResponseParser() {
        return new LLMResponseParser();
    }
}
