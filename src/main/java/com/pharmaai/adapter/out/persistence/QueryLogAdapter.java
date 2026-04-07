package com.pharmaai.adapter.out.persistence;

import com.pharmaai.domain.port.out.QueryLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QueryLogAdapter implements QueryLogPort {

    private static final Logger log = LoggerFactory.getLogger(QueryLogAdapter.class);

    private final QueryLogJpaRepository repository;

    public QueryLogAdapter(QueryLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(String userPhone, String userQuestion, String drugFound,
                    String contextUsed, String llmResponse, String riskLevel) {
        try {
            QueryLogEntity entity = new QueryLogEntity();
            entity.setUserPhone(userPhone);
            entity.setUserQuestion(userQuestion);
            entity.setDrugFound(drugFound);
            entity.setContextUsed(contextUsed);
            entity.setLlmResponse(llmResponse);
            entity.setRiskLevel(riskLevel);

            repository.save(entity);
            log.debug("📊 Query log salvo: pergunta='{}', drug='{}'", userQuestion, drugFound);
        } catch (Exception e) {
            log.error("❌ Erro ao salvar query log: {}", e.getMessage(), e);
        }
    }
}

