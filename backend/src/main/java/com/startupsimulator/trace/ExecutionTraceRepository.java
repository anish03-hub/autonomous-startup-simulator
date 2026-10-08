package com.startupsimulator.trace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for persisted execution trace entries.
 */
@Repository
public interface ExecutionTraceRepository extends JpaRepository<ExecutionTraceEntry, Long> {
    List<ExecutionTraceEntry> findByStartupIdOrderBySequenceNumberAsc(Long startupId);
    List<ExecutionTraceEntry> findByStartupIdAndTraceIdOrderBySequenceNumberAsc(Long startupId, String traceId);
    long countByStartupId(Long startupId);
}
