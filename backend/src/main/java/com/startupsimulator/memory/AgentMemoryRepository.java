package com.startupsimulator.memory;

import com.startupsimulator.model.enums.AgentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Phase 5A: the persistence seam for {@link AgentMemory}. Plain Spring Data,
 * exactly like {@link com.startupsimulator.repository.AgentMessageRepository} —
 * all retrieval is deterministic ({@code OrderByCreatedAtDescIdDesc}; the id
 * tiebreak keeps ordering stable even when two rows share an {@code Instant}).
 *
 * <p><b>Boundary rule (requirement 7/13):</b> agents must NOT depend on this
 * repository. Only {@link MemoryService} does — the service is the memory
 * boundary; reasoning code never sees a JPA type.
 */
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    /** Agent-specific retrieval: one agent's private memories, newest first. */
    List<AgentMemory> findByStartupIdAndScopeAndAgentTypeOrderByCreatedAtDescIdDesc(
            Long startupId, MemoryScope scope, AgentType agentType);

    /** Scope-based retrieval (e.g. shared memories), newest first. */
    List<AgentMemory> findByStartupIdAndScopeOrderByCreatedAtDescIdDesc(
            Long startupId, MemoryScope scope);

    /** Recent memories for a startup (all scopes/agents), newest first, bounded by {@code pageable}. */
    List<AgentMemory> findByStartupIdOrderByCreatedAtDescIdDesc(Long startupId, Pageable pageable);

    /** Memory-type filtered retrieval for a startup, newest first. */
    List<AgentMemory> findByStartupIdAndMemoryTypeOrderByCreatedAtDescIdDesc(
            Long startupId, MemoryType memoryType);

    /**
     * Deterministic exact-duplicate check (requirement 9): a memory identical in
     * startup + scope + author + type + content already exists. No embeddings, no
     * similarity — exact match only.
     */
    boolean existsByStartupIdAndScopeAndAgentTypeAndMemoryTypeAndContent(
            Long startupId, MemoryScope scope, AgentType agentType, MemoryType memoryType, String content);

    AgentMemory findFirstByStartupIdAndScopeAndAgentTypeAndMemoryTypeAndContentOrderByIdAsc(
            Long startupId, MemoryScope scope, AgentType agentType, MemoryType memoryType, String content);
}
