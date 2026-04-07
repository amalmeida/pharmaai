package com.pharmaai.adapter.out.persistence;

import com.pharmaai.domain.model.DrugInfo;
import org.springframework.stereotype.Component;

@Component
public class DrugEntityMapper {

    public DrugInfo toDomain(DrugEntity entity) {
        DrugInfo drug = new DrugInfo();
        drug.setName(entity.getName());
        drug.setIndication(entity.getIndication());
        drug.setMaxDose(entity.getMaxDose());
        drug.setContraindications(entity.getContraindications());
        drug.setSideEffects(entity.getSideEffects());
        return drug;
    }

    public DrugEntity toEntity(DrugInfo drug) {
        DrugEntity entity = new DrugEntity();
        entity.setName(drug.getName());
        entity.setIndication(drug.getIndication());
        entity.setMaxDose(drug.getMaxDose());
        entity.setContraindications(drug.getContraindications());
        entity.setSideEffects(drug.getSideEffects());
        return entity;
    }
}

