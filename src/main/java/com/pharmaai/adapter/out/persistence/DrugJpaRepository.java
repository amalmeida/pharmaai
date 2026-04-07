package com.pharmaai.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugJpaRepository extends JpaRepository<DrugEntity, Long> {

    Optional<DrugEntity> findByNameIgnoreCase(String name);

    @Query("SELECT d.name FROM DrugEntity d")
    List<String> findAllNames();
}

