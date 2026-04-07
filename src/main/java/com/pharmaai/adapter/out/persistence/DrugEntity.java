package com.pharmaai.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "drugs")
public class DrugEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String indication;

    @Column(name = "max_dose", columnDefinition = "TEXT")
    private String maxDose;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "drug_contraindications", joinColumns = @JoinColumn(name = "drug_id"))
    @Column(name = "contraindication", columnDefinition = "TEXT")
    private List<String> contraindications;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "drug_side_effects", joinColumns = @JoinColumn(name = "drug_id"))
    @Column(name = "side_effect", columnDefinition = "TEXT")
    private List<String> sideEffects;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndication() { return indication; }
    public void setIndication(String indication) { this.indication = indication; }

    public String getMaxDose() { return maxDose; }
    public void setMaxDose(String maxDose) { this.maxDose = maxDose; }

    public List<String> getContraindications() { return contraindications; }
    public void setContraindications(List<String> contraindications) { this.contraindications = contraindications; }

    public List<String> getSideEffects() { return sideEffects; }
    public void setSideEffects(List<String> sideEffects) { this.sideEffects = sideEffects; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

