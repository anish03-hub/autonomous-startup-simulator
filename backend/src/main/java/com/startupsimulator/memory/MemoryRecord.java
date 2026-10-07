package com.startupsimulator.memory;

import com.startupsimulator.model.enums.AgentType;

import java.time.Instant;

/**
 * Phase 5A: the read-model returned by {@link MemoryService} (requirement 19). It
 * is a plain immutable snapshot — the {@link AgentMemory} JPA entity is never
 * handed out, so future agent code reasons over DTOs and stays decoupled from
 * persistence.
 */
public record MemoryRecord(
        Long id,
        Long startupId,
        AgentType agentType,
        MemoryScope scope,
        MemoryType memoryType,
        String content,
        int importance,
        String source,
        String sourceReference,
        Instant createdAt,
        Instant updatedAt
) {

    /** Project a persisted entity into the read-model. */
    static MemoryRecord from(AgentMemory m) {
        return new MemoryRecord(
                m.getId(), m.getStartupId(), m.getAgentType(), m.getScope(),
                m.getMemoryType(), m.getContent(), m.getImportance(),
                m.getSource(), m.getSourceReference(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
