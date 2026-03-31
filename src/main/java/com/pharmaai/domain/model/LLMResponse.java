package com.pharmaai.domain.model;

import java.util.List;

public class LLMResponse {

    private String answer;
    private String risk_level;
    private List<String> alerts;
    private String recommendation;

    public String getAnswer() {
        return answer;
    }

    public String getRisk_level() {
        return risk_level;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public String getRecommendation() {
        return recommendation;
    }
}