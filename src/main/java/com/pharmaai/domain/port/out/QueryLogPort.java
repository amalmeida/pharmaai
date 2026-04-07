package com.pharmaai.domain.port.out;

public interface QueryLogPort {

    void log(String userPhone, String userQuestion, String drugFound,
             String contextUsed, String llmResponse, String riskLevel);
}

