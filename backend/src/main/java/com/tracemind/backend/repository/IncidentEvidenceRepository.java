package com.tracemind.backend.repository;

import com.tracemind.backend.entity.IncidentEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentEvidenceRepository extends JpaRepository<IncidentEvidenceEntity, String> {
    List<IncidentEvidenceEntity> findByIncidentId(String incidentId);
}
