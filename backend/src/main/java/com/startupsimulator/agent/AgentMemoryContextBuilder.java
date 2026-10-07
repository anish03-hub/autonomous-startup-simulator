package com.startupsimulator.agent;

import com.startupsimulator.memory.CreateMemoryRequest;
import com.startupsimulator.memory.MemoryRecord;
import com.startupsimulator.memory.MemoryScope;
import com.startupsimulator.memory.MemoryService;
import com.startupsimulator.memory.MemoryType;
import com.startupsimulator.memory.MemoryValidationException;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Phase 5B: the collaborator that turns persistent memory (Phase 5A) into
 * <em>reasoning context</em> and turns an agent's memory-creation intents into
 * persisted rows. It is the single seam between department-agent reasoning and
 * {@link MemoryService}; agents never touch {@link com.startupsimulator.memory.AgentMemoryRepository}.
 *
 * <p>Three cleanly separated responsibilities (requirement 4):
 * <ol>
 *   <li><b>retrieve + rank + bound</b> ({@link #selectRelevant}) — deterministic
 *       ordering (agent-private before shared, then importance desc, then recency,
 *       then a stable id tie-breaker), hard-capped at {@link #MAX_RELEVANT_MEMORIES};
 *       no embeddings, no vector search, no semantic ranking;</li>
 *   <li><b>format</b> ({@link #renderBlock}) — render the bounded set into the
 *       {@code === RELEVANT PERSISTENT MEMORY ===} prompt block (or the explicit
 *       "none" text), kept entirely separate from retrieval;</li>
 *   <li><b>persist intents</b> ({@link #persistIntents}) — bounded, validated,
 *       de-duplicated creation of agent-authored memories through
 *       {@link MemoryService#save} only, never bypassing its validation.</li>
 * </ol>
 *
 * <p>This component only reads and writes real rows. It never fabricates a memory
 * in Java, and when persistence is rejected it reports that (the caller does not
 * pretend a memory was stored).
 */
@Slf4j
@Service
public class AgentMemoryContextBuilder {

    /** Hard cap on memories injected into any single prompt — keeps prompts bounded. */
    public static final int MAX_RELEVANT_MEMORIES = 5;
    /** Hard cap on memories an agent may create from one successful analysis. */
    public static final int MAX_MEMORY_INTENTS_PER_ANALYSIS = 3;

    static final String HEADER = "=== RELEVANT PERSISTENT MEMORY ===";
    static final String NONE = "No relevant persistent memory was found.";

    private final MemoryService memoryService;
    private final EventService eventService;

    public AgentMemoryContextBuilder(MemoryService memoryService, EventService eventService) {
        this.memoryService = memoryService;
        this.eventService = eventService;
    }

    // ---- Retrieval + ranking (separate from formatting) ---------------------

    /**
     * Deterministic ranking: an agent's own private memories outrank the
     * startup's shared memories; within a tier, higher importance first, then
     * more recently created, then higher id as a stable final tie-breaker.
     */
    private static final Comparator<MemoryRecord> RANKING =
            Comparator.<MemoryRecord>comparingInt(r -> r.scope() == MemoryScope.AGENT_PRIVATE ? 0 : 1)
                    .thenComparing(MemoryRecord::importance, Comparator.reverseOrder())
                    .thenComparing(MemoryRecord::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MemoryRecord::id, Comparator.nullsLast(Comparator.reverseOrder()));

    /**
     * Select the bounded, ranked set of memories relevant to {@code agentType}
     * reasoning for {@code startupId}: that agent's private memories plus the
     * startup-shared memories, merged, ranked and capped at
     * {@link #MAX_RELEVANT_MEMORIES}. Emits one {@link EventType#MEMORY_RETRIEVED}
     * recording that a retrieval occurred (0 results is valid and still emitted).
     * Never throws; a null startup/agent yields an empty selection.
     */
    public List<MemoryRecord> selectRelevant(Long startupId, AgentType agentType) {
        if (startupId == null || agentType == null) {
            return List.of();
        }
        List<MemoryRecord> merged = new ArrayList<>();
        merged.addAll(memoryService.findForAgent(startupId, agentType));   // AGENT_PRIVATE
        merged.addAll(memoryService.findShared(startupId));                 // STARTUP_SHARED
        merged.sort(RANKING);
        List<MemoryRecord> bounded = merged.size() > MAX_RELEVANT_MEMORIES
                ? List.copyOf(merged.subList(0, MAX_RELEVANT_MEMORIES))
                : List.copyOf(merged);

        eventService.record(startupId, EventType.MEMORY_RETRIEVED,
                agentType.getDisplayName() + " retrieved " + bounded.size()
                        + " relevant persistent memor" + (bounded.size() == 1 ? "y" : "ies") + ".",
                Map.of("agent", agentType.name(), "count", bounded.size()));
        return bounded;
    }

    // ---- Formatting (separate from retrieval) -------------------------------

    /**
     * Render a bounded, already-selected set of memories into the Phase 5B prompt
     * block. When the set is empty, render the explicit "none" text — never
     * invent memory. The §15 guidance frames memory as prior-reasoning evidence,
     * not unquestionable truth, and current authoritative context wins on conflict.
     */
    public String renderBlock(List<MemoryRecord> memories) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        if (memories == null || memories.isEmpty()) {
            sb.append(NONE).append('\n');
            return sb.toString();
        }
        sb.append("These are durable notes recorded during earlier reasoning — your own private notes "
                + "and shared startup memory. Treat them as useful prior evidence, not unquestionable "
                + "truth; if any conflicts with the authoritative context above, prefer the current "
                + "context. Only the memories listed here are real — do not invent others.\n");
        for (MemoryRecord m : memories) {
            sb.append("- [")
                    .append(m.memoryType()).append(" | importance ").append(m.importance())
                    .append(" | ").append(m.scope())
                    .append(" | created ").append(m.createdAt());
            if (m.source() != null && !m.source().isBlank()) {
                sb.append(" | source: ").append(m.source().trim());
            }
            sb.append("]\n  ").append(m.content() == null ? "" : m.content().trim()).append('\n');
        }
        return sb.toString();
    }

    /** Convenience: retrieve + rank + bound + render in one call. */
    public String relevantMemoryBlock(Long startupId, AgentType agentType) {
        return renderBlock(selectRelevant(startupId, agentType));
    }

    // ---- Agent-driven creation (bounded, validated, de-duplicated) ----------

    /**
     * Persist the memories an agent intentionally chose to record from a
     * <em>successful</em> analysis. Deterministic bounds and validation
     * (requirements 8/9): at most {@link #MAX_MEMORY_INTENTS_PER_ANALYSIS} rows
     * are created; empty intents and intents naming a type outside the closed
     * {@link MemoryType} vocabulary are skipped; everything else is routed through
     * {@link MemoryService#save}, which applies the Phase 5A validation and exact
     * de-duplication (an identical candidate creates no second row and emits no
     * second event). Never throws; a rejected candidate is logged and skipped, and
     * the returned list reflects exactly what was persisted or already existed.
     */
    public List<MemoryRecord> persistIntents(Long startupId, AgentType agentType,
                                             List<AgentMemoryIntent> intents) {
        if (startupId == null || agentType == null || intents == null || intents.isEmpty()) {
            return List.of();
        }
        List<MemoryRecord> persisted = new ArrayList<>();
        int created = 0;
        for (AgentMemoryIntent intent : intents) {
            if (created >= MAX_MEMORY_INTENTS_PER_ANALYSIS) {
                break;                                   // deterministic truncation
            }
            if (intent == null || !intent.isPresent()) {
                continue;                                // nothing worth recording
            }
            MemoryType type = intent.resolvedType();
            if (type == null) {
                log.debug("Memory intent skipped: unmappable memoryType '{}'.", intent.memoryType());
                continue;                                // not an in-vocabulary type → no row
            }
            CreateMemoryRequest request = new CreateMemoryRequest(
                    startupId, agentType, intent.resolvedScope(), type,
                    intent.normalizedContent(), intent.clampedImportance(),
                    agentType.name().toLowerCase() + "-analysis", null);
            try {
                persisted.add(memoryService.save(request));   // reuse Phase 5A validation + dedup + event
                created++;
            } catch (MemoryValidationException e) {
                // Persistence genuinely failed validation — do NOT pretend success.
                log.warn("Agent memory intent rejected ({}): {}", e.code(), e.getMessage());
            }
        }
        return persisted;
    }
}
