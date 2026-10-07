package com.startupsimulator.memory;

import com.startupsimulator.model.enums.AgentType;

/**
 * Phase 5A: a typed, intention-revealing request to create a memory
 * (requirement 19). This is what reasoning code builds — it never touches the
 * {@link AgentMemory} JPA entity. Memory creation is always <b>explicit</b>
 * (requirement 11): a caller constructs one of these on purpose; nothing here is
 * produced automatically from an LLM response.
 *
 * @param startupId       which startup the memory belongs to (required)
 * @param agentType       the authoring agent (required even for shared memory)
 * @param scope           visibility — {@link MemoryScope#AGENT_PRIVATE} or
 *                        {@link MemoryScope#STARTUP_SHARED} (required)
 * @param memoryType      what kind of knowledge (required)
 * @param content         the meaningful knowledge (required, non-blank, bounded)
 * @param importance      caller-supplied significance (validated 1–10)
 * @param source          optional provenance note (may be null)
 * @param sourceReference optional pointer to the originating artefact (may be null)
 */
public record CreateMemoryRequest(
        Long startupId,
        AgentType agentType,
        MemoryScope scope,
        MemoryType memoryType,
        String content,
        int importance,
        String source,
        String sourceReference
) {

    /** Convenience factory for an agent-private memory with default importance 5. */
    public static CreateMemoryRequest privateMemory(Long startupId, AgentType agentType,
                                                    MemoryType memoryType, String content) {
        return new CreateMemoryRequest(startupId, agentType, MemoryScope.AGENT_PRIVATE,
                memoryType, content, 5, null, null);
    }

    /** Convenience factory for a startup-shared memory with default importance 5. */
    public static CreateMemoryRequest sharedMemory(Long startupId, AgentType author,
                                                   MemoryType memoryType, String content) {
        return new CreateMemoryRequest(startupId, author, MemoryScope.STARTUP_SHARED,
                memoryType, content, 5, null, null);
    }

    /** Trimmed content, or null when unset — the normalised value that is persisted. */
    public String normalizedContent() {
        return content == null ? null : content.strip();
    }
}
