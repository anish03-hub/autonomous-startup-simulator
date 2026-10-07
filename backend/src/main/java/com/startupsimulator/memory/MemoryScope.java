package com.startupsimulator.memory;

/**
 * Phase 5A: the <em>ownership/visibility</em> dimension of a memory, kept
 * explicit and separate from the authoring {@code agentType} so shared knowledge
 * is one canonical row (never copied once per agent — see requirement 17).
 *
 * <p>Persisted with {@link jakarta.persistence.EnumType#STRING}.
 */
public enum MemoryScope {

    /**
     * Private to a single owning agent: retrievable only when that agent is
     * explicitly requested. One department's private memory never leaks into
     * another department's agent-specific retrieval (requirement 15/16).
     */
    AGENT_PRIVATE,

    /**
     * Shared across the whole startup: a single canonical row retrievable through
     * this declared scope by any agent that explicitly asks for shared memory. The
     * {@code agentType} field still records which agent authored it.
     */
    STARTUP_SHARED
}
