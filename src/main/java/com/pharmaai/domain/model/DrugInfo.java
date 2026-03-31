package com.pharmaai.domain.model;

import java.util.List;

public class DrugInfo {

    private String name;
    private String indication;
    private String maxDose;
    private List<String> contraindications;
    private List<String> sideEffects;

    // 🔹 GETTERS
    public String getName() {
        return name;
    }

    public String getIndication() {
        return indication;
    }

    public String getMaxDose() {
        return maxDose;
    }

    public List<String> getContraindications() {
        return contraindications;
    }

    public List<String> getSideEffects() {
        return sideEffects;
    }

    // 🔹 SETTERS
    public void setName(String name) {
        this.name = name;
    }

    public void setIndication(String indication) {
        this.indication = indication;
    }

    public void setMaxDose(String maxDose) {
        this.maxDose = maxDose;
    }

    public void setContraindications(List<String> contraindications) {
        this.contraindications = contraindications;
    }

    public void setSideEffects(List<String> sideEffects) {
        this.sideEffects = sideEffects;
    }
}