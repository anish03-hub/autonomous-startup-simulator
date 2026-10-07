package com.startupsimulator.memory;

import com.startupsimulator.model.enums.AgentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Phase 5A: a single piece of durable knowledge produced during a simulation run
 * that <b>persists beyond that run</b> and can be retrieved later. It is a real
 * relational row (table {@code agent_memories}) — never a Java field, static, an
 * in-memory list, a {@code StartupContext}, an {@code AgentMessage}, or a prompt
 * string. It survives the end of the run that created it.
 *
 * <p>Mirrors the project's established JPA conventions (see
 * {@link com.startupsimulator.model.AgentMessage} /
 * {@link com.startupsimulator.model.StartupEvent}): {@code IDENTITY} id,
 * {@code EnumType.STRING} enums, {@code TEXT} content, an immutable
 * {@code createdAt}. The schema is produced by {@code ddl-auto} (no migration
 * tooling in this project — see {@code application.yml}).
 *
 * <p>Every memory answers five questions: WHICH startup ({@link #startupId}),
 * WHO authored it ({@link #agentType}), WHO can see it ({@link #scope}), WHAT
 * KIND it is ({@link #memoryType}) and WHEN it was formed ({@link #createdAt}).
 *
 * <p>Persistence is the only responsibility here; reasoning never touches this
 * entity directly — it goes through {@link MemoryService} and the typed DTOs.
 */
@Entity
@Table(name = "agent_memories", indexes = {
        @Index(name = "idx_memories_startup", columnList = "startup_id"),
        @Index(name = "idx_memories_startup_scope_agent", columnList = "startup_id, scope, agent_type")
})
@Getter
@Setter
@NoArgsConstructor
public class AgentMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** WHICH startup this memory belongs to — the hard isolation boundary. */
    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    /** WHO authored the memory (the owner for {@link MemoryScope#AGENT_PRIVATE}). */
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 32)
    private AgentType agentType;

    /** WHO can retrieve it: private to the author, or shared across the startup. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private MemoryScope scope;

    /** WHAT KIND of knowledge this is — a filterable, deterministic dimension. */
    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 32)
    private MemoryType memoryType;

    /** The actual, meaningful knowledge (not "agent completed"). Stored as TEXT. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Caller-supplied significance (validated 1–10 by {@link MemoryService}). Not
     * used for ranking in Phase 5A — stored so later phases can prioritise without
     * a schema change.
     */
    @Column(name = "importance", nullable = false)
    private int importance;

    /**
     * Optional provenance: a short, free-form note of where the memory came from
     * (e.g. "boardroom-debate", "finance-analysis"). Nullable.
     */
    @Column(name = "source", length = 64)
    private String source;

    /**
     * Optional pointer back to the originating artefact (e.g. a debate id or event
     * id, as a string so it is storage-agnostic). Nullable.
     */
    @Column(name = "source_reference", length = 128)
    private String sourceReference;

    /** WHEN it was formed — immutable. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Last modification time (equals {@link #createdAt} until a memory is ever edited). */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AgentMemory(Long startupId, AgentType agentType, MemoryScope scope,
                       MemoryType memoryType, String content, int importance,
                       String source, String sourceReference) {
        this.startupId = startupId;
        this.agentType = agentType;
        this.scope = scope;
        this.memoryType = memoryType;
        this.content = content;
        this.importance = importance;
        this.source = source;
        this.sourceReference = sourceReference;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
