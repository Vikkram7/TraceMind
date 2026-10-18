package com.tracemind.backend.repository;

import com.tracemind.backend.entity.RootCauseAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RootCauseAnalysisRepository extends JpaRepository<RootCauseAnalysisEntity, String> {
    Optional<RootCauseAnalysisEntity> findByIncidentId(String incidentId);
}
