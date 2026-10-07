package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.startupsimulator.memory.MemoryScope;
import com.startupsimulator.memory.MemoryType;

/**
 * Phase 5B: an optional, agent-driven memory-creation intent produced by an
 * agent's LLM <em>within</em> its existing structured response — NOT a
 * tool/function call and NOT automatic. The model chooses to record a durable,
 * future-useful memory (a validated financial assumption, a product constraint,
 * a strategic decision, a recurring technical limitation, a market insight, a
 * cross-department decision, an unresolved risk) by emitting one of these.
 *
 * <p>The agent only expresses the intent; {@link com.startupsimulator.memory.MemoryService}
 * (reached through {@link com.startupsimulator.memory.AgentMemoryContextBuilder})
 * is solely responsible for validating it, de-duplicating it and persisting the
 * row. Nothing here writes to the database or fabricates a memory. An absent or
 * incomplete intent means the agent chose not to record anything this turn.
 *
 * <p>Scope and memory type arrive as raw strings so a slightly loose model
 * response still parses; they are resolved leniently to the <em>existing</em>
 * {@link MemoryScope} / {@link MemoryType} vocabularies (Phase 5A), defaulting to
 * {@link MemoryScope#AGENT_PRIVATE} and rejecting an unmappable type (no row).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentMemoryIntent(
        String scope,
        String memoryType,
        String content,
        Integer importance
) {

    /** True only when the agent actually wrote memory content worth persisting. */
    public boolean isPresent() {
        return notBlank(content);
    }

    public String normalizedContent() {
        return content == null ? null : content.trim();
    }

    /** Lenient resolution to the existing scope vocabulary; defaults to private. */
    public MemoryScope resolvedScope() {
        if (notBlank(scope)) {
            String s = scope.trim().toUpperCase();
            for (MemoryScope candidate : MemoryScope.values()) {
                if (candidate.name().equals(s)) {
                    return candidate;
                }
            }
            if (s.contains("SHARED") || s.contains("STARTUP")) {
                return MemoryScope.STARTUP_SHARED;
            }
        }
        return MemoryScope.AGENT_PRIVATE;
    }

    /**
     * Lenient resolution to the existing {@link MemoryType} vocabulary. Returns
     * {@code null} when the model named a type outside the closed Phase 5A set,
     * so the caller skips it rather than inventing a type or writing a bad row.
     */
    public MemoryType resolvedType() {
        if (!notBlank(memoryType)) {
            return null;
        }
        String t = memoryType.trim().toUpperCase();
        for (MemoryType candidate : MemoryType.values()) {
            if (candidate.name().equals(t)) {
                return candidate;
            }
        }
        return null;
    }

    /** Clamp importance into the Phase 5A range; default mid-weight when absent. */
    public int clampedImportance() {
        int value = importance == null ? 5 : importance;
        return Math.max(1, Math.min(10, value));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
