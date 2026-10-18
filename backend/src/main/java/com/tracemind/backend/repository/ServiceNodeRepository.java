package com.tracemind.backend.repository;

import com.tracemind.backend.entity.ServiceNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceNodeRepository extends JpaRepository<ServiceNodeEntity, String> {
    Optional<ServiceNodeEntity> findByName(String name);
}
