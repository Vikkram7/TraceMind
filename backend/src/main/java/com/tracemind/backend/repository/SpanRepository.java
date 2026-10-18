package com.tracemind.backend.repository;

import com.tracemind.backend.entity.SpanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpanRepository extends JpaRepository<SpanEntity, String> {

    List<SpanEntity> findByTraceIdOrderByStartTimeAsc(String traceId);

    List<SpanEntity> findByServiceName(String serviceName);

    List<SpanEntity> findTop50ByIsErrorTrueOrderByStartTimeDesc();

    long countByServiceName(String serviceName);

    long countByServiceNameAndIsErrorTrue(String serviceName);

    @Query("SELECT AVG(s.durationMs) FROM SpanEntity s WHERE s.serviceName = :serviceName")
    Double findAvgDurationByServiceName(@Param("serviceName") String serviceName);

    @Query("SELECT s.durationMs FROM SpanEntity s WHERE s.serviceName = :serviceName ORDER BY s.durationMs ASC")
    List<Double> findDurationsByServiceName(@Param("serviceName") String serviceName);
}
