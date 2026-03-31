package com.pharmaai.adapter.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaai.domain.model.DrugInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DrugCacheRepository {

    private static final String FILE_PATH = "drugs-cache.json";

    private final ObjectMapper mapper = new ObjectMapper();

    public List<DrugInfo> load() {
        try {
            File file = new File(FILE_PATH);

            if (!file.exists()) {
                return new ArrayList<>();
            }

            return mapper.readValue(file, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar cache", e);
        }
    }

    public void save(List<DrugInfo> drugs) {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(FILE_PATH), drugs);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar cache", e);
        }
    }
}