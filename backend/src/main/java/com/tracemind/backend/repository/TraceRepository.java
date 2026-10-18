package com.tracemind.backend.repository;

import com.tracemind.backend.entity.TraceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TraceRepository extends JpaRepository<TraceEntity, String> {

    Page<TraceEntity> findAllByOrderByStartTimeDesc(Pageable pageable);

    Page<TraceEntity> findByRootServiceOrderByStartTimeDesc(String rootService, Pageable pageable);

    Page<TraceEntity> findByStatusOrderByStartTimeDesc(String status, Pageable pageable);

    @Query("SELECT t FROM TraceEntity t WHERE " +
           "(:service IS NULL OR t.rootService = :service) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:minDuration IS NULL OR t.durationMs >= :minDuration) " +
           "ORDER BY t.startTime DESC")
    Page<TraceEntity> searchTraces(
            @Param("service") String service,
            @Param("status") String status,
            @Param("minDuration") Double minDuration,
            Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT AVG(t.durationMs) FROM TraceEntity t")
    Double findAverageDurationMs();

    List<TraceEntity> findTop100ByOrderByStartTimeDesc();

    List<TraceEntity> findTop50ByStatusOrderByStartTimeDesc(String status);
}
