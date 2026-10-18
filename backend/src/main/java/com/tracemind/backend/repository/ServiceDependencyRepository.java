package com.tracemind.backend.repository;

import com.tracemind.backend.entity.ServiceDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceDependencyRepository extends JpaRepository<ServiceDependencyEntity, String> {
    Optional<ServiceDependencyEntity> findBySourceServiceAndTargetService(String sourceService, String targetService);
    List<ServiceDependencyEntity> findBySourceService(String sourceService);
    List<ServiceDependencyEntity> findByTargetService(String targetService);
}
