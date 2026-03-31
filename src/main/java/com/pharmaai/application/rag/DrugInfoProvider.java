package com.pharmaai.application.rag;

import com.pharmaai.domain.model.DrugInfo;

import java.util.Optional;

public interface DrugInfoProvider {

    Optional<DrugInfo> findByName(String name);

}