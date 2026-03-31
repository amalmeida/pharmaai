package com.pharmaai.application.rag;

import com.pharmaai.domain.model.DrugInfo;

import java.util.stream.Collectors;

public class ContextFormatter {

    private static final int MAX_LENGTH = 1500;

    public String format(DrugInfo drug) {

        return """
                Nome: %s
                Indicação: %s
                Posologia (dosagem): %s
                Contraindicações: %s
                Alertas e efeitos colaterais: %s
                """.formatted(
                safe(drug.getName()),
                truncate(drug.getIndication()),
                truncate(drug.getMaxDose()),
                truncateList(drug.getContraindications()),
                truncateList(drug.getSideEffects())
        );
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > MAX_LENGTH
                ? text.substring(0, MAX_LENGTH) + "..."
                : text;
    }

    private String truncateList(java.util.List<String> list) {
        if (list == null) return "";
        return list.stream()
                .map(this::truncate)
                .collect(Collectors.joining("; "));
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}