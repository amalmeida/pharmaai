package com.pharmaai.config;

import com.pharmaai.adapter.out.persistence.DrugEntity;
import com.pharmaai.adapter.out.persistence.DrugEntityMapper;
import com.pharmaai.adapter.out.persistence.DrugJpaRepository;
import com.pharmaai.domain.model.DrugInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);
    private static final String CACHE_FILE = "drugs-cache.json";

    private final DrugJpaRepository drugRepository;
    private final DrugEntityMapper mapper;

    public DataMigrationRunner(DrugJpaRepository drugRepository, DrugEntityMapper mapper) {
        this.drugRepository = drugRepository;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) {
        if (drugRepository.count() > 0) {
            log.info("📦 Banco já contém {} medicamentos. Migração ignorada.", drugRepository.count());
            return;
        }

        File cacheFile = new File(CACHE_FILE);
        if (!cacheFile.exists()) {
            log.info("📦 Arquivo {} não encontrado. Nenhum dado para migrar.", CACHE_FILE);
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<DrugInfo> drugs = objectMapper.readValue(cacheFile, new TypeReference<>() {});

            int migrated = 0;
            for (DrugInfo drug : drugs) {
                try {
                    DrugEntity entity = mapper.toEntity(drug);
                    drugRepository.save(entity);
                    migrated++;
                    log.info("✅ Migrado: {}", drug.getName());
                } catch (Exception e) {
                    log.warn("⚠️ Erro ao migrar {}: {}", drug.getName(), e.getMessage());
                }
            }

            log.info("📦 Migração concluída: {}/{} medicamentos importados do cache JSON", migrated, drugs.size());

        } catch (Exception e) {
            log.error("❌ Erro na migração do cache: {}", e.getMessage(), e);
        }
    }
}

