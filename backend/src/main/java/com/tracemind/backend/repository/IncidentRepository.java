package com.tracemind.backend.repository;

import com.tracemind.backend.entity.IncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, String> {

    Page<IncidentEntity> findAllByOrderByFirstSeenDesc(Pageable pageable);

    List<IncidentEntity> findByStatusOrderByFirstSeenDesc(String status);

    List<IncidentEntity> findByServiceNameOrderByFirstSeenDesc(String serviceName);

    Optional<IncidentEntity> findFirstByServiceNameAndDetectionRuleAndStatus(
            String serviceName, String detectionRule, String status);

    long countByStatus(String status);
}
