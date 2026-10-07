package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.agent.AgentMemoryContextBuilder;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.SimulationPhase;
import com.startupsimulator.orchestrator.decision.OrchestrationStateSnapshot.CompletedAnalysis;
import com.startupsimulator.repository.AgentMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CA3 Phase 6A: assembles a bounded, deterministic {@link OrchestrationStateSnapshot}
 * from information that <em>actually exists</em> — never fabricated (requirement 7).
 * It is the single place that wires the three distinct, deliberately-separate
 * context sources into the snapshot the engine reasons over:
 *
 * <ul>
 *   <li><b>persistent memory</b> — via the existing Phase 5B
 *       {@link AgentMemoryContextBuilder#relevantMemoryBlock} (requirement 8). The
 *       orchestration perspective is the CEO: it surfaces CEO-private plus
 *       startup-shared memory, bounded and ranked by the existing retrieval, and
 *       emits the usual {@code MEMORY_RETRIEVED} event — no new memory path, no
 *       bypass of {@code MemoryService};</li>
 *   <li><b>agent communication</b> — via the existing
 *       {@link AgentMessageRepository} (requirement 9), bounded to the most recent
 *       {@link #MAX_RECENT_MESSAGES}. Messages and memory are kept as separate
 *       inputs and never collapsed into one abstraction;</li>
 *   <li><b>authoritative simulation state</b> — the completed/pending agents,
 *       debate/execution status and prior orchestration decisions are supplied
 *       explicitly by the caller (the orchestrator knows them), so the snapshot is
 *       deterministic and unit-testable rather than inferred from fuzzy fields.</li>
 * </ul>
 *
 * <p>This factory builds state only; it never asks the LLM or decides anything.
 */
@Service
public class OrchestrationStateSnapshotFactory {

    /** Upper bound on inter-department messages surfaced to the decision prompt. */
    public static final int MAX_RECENT_MESSAGES = 10;

    private final AgentMemoryContextBuilder memoryContextBuilder;
    private final AgentMessageRepository messageRepository;

    public OrchestrationStateSnapshotFactory(AgentMemoryContextBuilder memoryContextBuilder,
                                             AgentMessageRepository messageRepository) {
        this.memoryContextBuilder = memoryContextBuilder;
        this.messageRepository = messageRepository;
    }

    /**
     * Build the snapshot for one orchestration decision. The caller supplies the
     * authoritative progress state (completed/pending/debate/execution + prior
     * decisions and completed-analysis headlines); this factory enriches it with
     * the bounded relevant memory and the recent inter-department messages pulled
     * from the real stores.
     */
    public OrchestrationStateSnapshot build(StartupContext ctx,
                                            SimulationPhase phase,
                                            List<AgentType> completedAgents,
                                            List<AgentType> pendingAgents,
                                            List<CompletedAnalysis> recentAnalyses,
                                            boolean debateStarted,
                                            boolean debateComplete,
                                            String executionStatus,
                                            List<OrchestrationDecision> previousDecisions) {
        Long startupId = ctx.startupId();

        // Requirement 8: relevant memory via the EXISTING builder (CEO perspective:
        // CEO-private + startup-shared), bounded/ranked by Phase 5B, emits MEMORY_RETRIEVED.
        String relevantMemory = memoryContextBuilder.relevantMemoryBlock(startupId, AgentType.CEO);

        // Requirement 9: recent inter-department messages from the real repository,
        // bounded to the most recent few (kept DISTINCT from memory).
        List<AgentMessage> recentMessages =
                tail(messageRepository.findByStartupIdOrderById(startupId), MAX_RECENT_MESSAGES);

        OrchestrationStateSnapshot.Builder builder = OrchestrationStateSnapshot.builder(startupId)
                .currentPhase(phase)
                .completed(completedAgents)
                .pending(pendingAgents)
                .messages(recentMessages)
                .relevantMemory(relevantMemory)
                .debateStarted(debateStarted)
                .debateComplete(debateComplete)
                .executionStatus(executionStatus)
                .previousDecisions(previousDecisions);

        if (recentAnalyses != null) {
            for (CompletedAnalysis a : recentAnalyses) {
                if (a != null) {
                    builder.analysis(a.agent(), a.headline());
                }
            }
        }
        return builder.build();
    }

    private static <T> List<T> tail(List<T> all, int max) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        return all.size() <= max ? all : all.subList(all.size() - max, all.size());
    }
}
