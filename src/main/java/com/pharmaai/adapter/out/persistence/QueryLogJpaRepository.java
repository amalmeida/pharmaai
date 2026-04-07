package com.pharmaai.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryLogJpaRepository extends JpaRepository<QueryLogEntity, Long> {
}

