package com.pharmaai.adapter.rag;

import com.pharmaai.application.rag.DrugInfoProvider;
import com.pharmaai.domain.model.DrugInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class HybridDrugInfoProvider implements DrugInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(HybridDrugInfoProvider.class);

    private final DrugCacheRepository cacheRepository;
    private final DailyMedClient client;
    private final DailyMedMapper mapper;

    public HybridDrugInfoProvider() {
        this(new DrugCacheRepository(), new DailyMedClient(), new DailyMedMapper());
    }

    public HybridDrugInfoProvider(DrugCacheRepository cacheRepository,
                                   DailyMedClient client,
                                   DailyMedMapper mapper) {
        this.cacheRepository = cacheRepository;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public Optional<DrugInfo> findByName(String name) {

        String normalized = name.toLowerCase();

        // 🔍 1. Busca no cache local
        List<DrugInfo> cache = cacheRepository.load();

        Optional<DrugInfo> cached = cache.stream()
                .filter(d -> normalized.contains(d.getName().toLowerCase()))
                .findFirst();

        if (cached.isPresent()) {
            log.info("✅ Cache HIT: {}", name);
            return cached;
        }

        log.info("🌐 Cache MISS - buscando no DailyMed: {}", name);

        try {
            // 🌐 2. Buscar setIds no DailyMed (até 5 resultados)
            List<String> setIds = client.findSetIds(name);

            if (setIds.isEmpty()) {
                log.warn("❌ Nenhum resultado no DailyMed");
                return Optional.empty();
            }

            // 🔄 3. Tentar cada SPL até encontrar um com conteúdo real
            for (int i = 0; i < setIds.size(); i++) {
                String setId = setIds.get(i);
                log.info("🔍 Tentando SPL {}/{}: {}", i + 1, setIds.size(), setId);

                String xml = client.getDrugDetails(setId);

                if (xml == null) {
                    log.debug("  ⏭️ XML nulo, tentando próximo...");
                    continue;
                }

                DrugInfo drug = mapper.mapFromXml(xml, name);

                if (drug == null || !mapper.hasContent(drug)) {
                    log.debug("  ⏭️ SPL sem conteúdo textual, tentando próximo...");
                    continue;
                }

                // ✅ Encontrou SPL com conteúdo!
                log.info("✅ SPL com conteúdo encontrado na tentativa {}", i + 1);

                // 💾 4. Salvar no cache local
                cache.add(drug);
                cacheRepository.save(cache);

                log.info("💾 Salvo no cache: {}", name);

                return Optional.of(drug);
            }

            log.warn("❌ Nenhum SPL com conteúdo textual encontrado após {} tentativas", setIds.size());
            return Optional.empty();

        } catch (Exception e) {
            log.error("❌ Erro ao consultar DailyMed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}