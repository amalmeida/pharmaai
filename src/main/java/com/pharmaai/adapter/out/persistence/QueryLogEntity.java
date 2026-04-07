package com.pharmaai.adapter.out.persistence;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "query_logs")
public class QueryLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_phone")
    private String userPhone;
    @Column(name = "user_question", columnDefinition = "TEXT", nullable = false)
    private String userQuestion;
    @Column(name = "drug_found")
    private String drugFound;
    @Column(name = "context_used", columnDefinition = "TEXT")
    private String contextUsed;
    @Column(name = "llm_response", columnDefinition = "TEXT")
    private String llmResponse;
    @Column(name = "risk_level")
    private String riskLevel;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserQuestion() { return userQuestion; }
    public void setUserQuestion(String userQuestion) { this.userQuestion = userQuestion; }
    public String getDrugFound() { return drugFound; }
    public void setDrugFound(String drugFound) { this.drugFound = drugFound; }
    public String getContextUsed() { return contextUsed; }
    public void setContextUsed(String contextUsed) { this.contextUsed = contextUsed; }
    public String getLlmResponse() { return llmResponse; }
    public void setLlmResponse(String llmResponse) { this.llmResponse = llmResponse; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
