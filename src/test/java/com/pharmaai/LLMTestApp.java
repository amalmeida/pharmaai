package com.pharmaai;

import com.pharmaai.adapter.out.llm.OpenAILLMAdapter;
import com.pharmaai.adapter.rag.DrugCacheRepository;
import com.pharmaai.adapter.rag.HybridDrugInfoProvider;
import com.pharmaai.application.parser.LLMResponseParser;
import com.pharmaai.application.prompt.PromptBuilderV2;
import com.pharmaai.application.rag.DrugKnowledgeService;
import com.pharmaai.config.OpenAIConfig;
import com.pharmaai.domain.model.LLMResponse;
import com.pharmaai.domain.port.out.LLMPort;

import java.util.List;

public class LLMTestApp {

    public static void main(String[] args) {

        // 🔑 CONFIG (sem Spring) — usar variável de ambiente
        OpenAIConfig config = new OpenAIConfig();
        config.setKey(System.getenv("OPENAI_API_KEY"));
        config.setUrl("https://api.openai.com/v1/chat/completions");
        config.setModel("gpt-4o-mini");

        LLMPort llm = new OpenAILLMAdapter(config);

        PromptBuilderV2 promptBuilder = new PromptBuilderV2();
        LLMResponseParser parser = new LLMResponseParser();

        HybridDrugInfoProvider provider = new HybridDrugInfoProvider();

        DrugCacheRepository cacheRepository = new DrugCacheRepository();

        List<String> knownDrugs = cacheRepository.load()
                .stream()
                .map(d -> d.getName())
                .toList();

        if (knownDrugs.isEmpty()) {
            knownDrugs = List.of("paracetamol", "dipirona", "ibuprofeno");
        }

        DrugKnowledgeService knowledgeService =
                new DrugKnowledgeService(provider, knownDrugs, llm);

        String userInput = "Qual a dose recomendada de Enterogermina  e para que serve?";

        System.out.println("🧪 Pergunta: " + userInput);

        String contextStr = knowledgeService.getContext(userInput);

        System.out.println("\n📚 CONTEXTO:");
        System.out.println(contextStr);

        String prompt = promptBuilder.buildWithContext(userInput, contextStr);

        String raw = llm.ask(prompt);

        LLMResponse response = parser.parse(raw);

        System.out.println("\n✅ RESPOSTA:");
        System.out.println(response.getAnswer());
    }
}