package com.startupsimulator.memory;

import com.startupsimulator.model.enums.AgentType;

/**
 * Phase 5A: a typed retrieval request (requirement 19). It names the scope of a
 * read explicitly so a caller can never accidentally ask for "everything" — the
 * {@link Kind} makes the intent (one agent's private memories vs the startup's
 * shared memories vs recent vs by-type) part of the type.
 *
 * <p>Deliberately minimal: no sorting knobs (ordering is always deterministic
 * newest-first) and no semantic parameters.
 */
public record MemoryQuery(
        Kind kind,
        Long startupId,
        AgentType agentType,
        MemoryType memoryType,
        int limit
) {

    /** The four deterministic retrieval shapes Phase 5A supports. */
    public enum Kind { FOR_AGENT, SHARED, RECENT, BY_TYPE }

    public static MemoryQuery forAgent(Long startupId, AgentType agentType) {
        return new MemoryQuery(Kind.FOR_AGENT, startupId, agentType, null, 0);
    }

    public static MemoryQuery shared(Long startupId) {
        return new MemoryQuery(Kind.SHARED, startupId, null, null, 0);
    }

    public static MemoryQuery recent(Long startupId, int limit) {
        return new MemoryQuery(Kind.RECENT, startupId, null, null, limit);
    }

    public static MemoryQuery byType(Long startupId, MemoryType memoryType) {
        return new MemoryQuery(Kind.BY_TYPE, startupId, null, memoryType, 0);
    }
}
