package com.tracemind.backend.repository;

import com.tracemind.backend.entity.ReplayRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplayRunRepository extends JpaRepository<ReplayRunEntity, String> {
    List<ReplayRunEntity> findAllByOrderByStartedAtDesc();
}
