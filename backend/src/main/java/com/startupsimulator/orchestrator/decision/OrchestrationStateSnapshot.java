package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.SimulationPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * CA3 Phase 6A: a deterministic, typed, read-only snapshot of exactly the
 * simulation state the orchestration decision engine is allowed to reason over
 * (requirement 7). It is assembled from information that <em>actually exists</em>
 * on the {@code StartupContext}/stores — nothing here is fabricated — and is the
 * single input (besides the system prompt) to the LLM decision call.
 *
 * <p>It also computes the current <b>legal</b> next actions (requirement 10) so
 * both the prompt (what the model is told it may do) and the
 * {@link OrchestrationDecisionValidator} (what Java will accept) derive legality
 * from one place.
 */
public record OrchestrationStateSnapshot(
        Long startupId,
        SimulationPhase currentPhase,
        List<AgentType> completedAgents,
        List<AgentType> pendingAgents,
        List<CompletedAnalysis> recentAnalyses,
        List<AgentMessage> recentMessages,
        String relevantMemory,
        boolean debateStarted,
        boolean debateComplete,
        String executionStatus,
        List<OrchestrationDecision> previousDecisions
) {

    /** The full set of departments; a debate requires all four to have run. */
    public static final List<AgentType> ALL_AGENTS =
            List.of(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE);

    /** A completed department analysis headline, for the COMPLETED ANALYSES block. */
    public record CompletedAnalysis(AgentType agent, String headline) {}

    public OrchestrationStateSnapshot {
        completedAgents = completedAgents == null ? List.of() : List.copyOf(completedAgents);
        pendingAgents = pendingAgents == null ? List.of() : List.copyOf(pendingAgents);
        recentAnalyses = recentAnalyses == null ? List.of() : List.copyOf(recentAnalyses);
        recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
        previousDecisions = previousDecisions == null ? List.of() : List.copyOf(previousDecisions);
        relevantMemory = relevantMemory == null ? "" : relevantMemory;
        executionStatus = executionStatus == null ? "NOT_STARTED" : executionStatus;
    }

    // ---- Legality (one source of truth for prompt + validator) --------------

    /**
     * The agents it is currently legal to RUN_AGENT: the pending (not-yet-run)
     * departments. Running an already-completed agent is not offered.
     */
    public List<AgentType> legalRunAgentTargets() {
        return pendingAgents;
    }

    /**
     * START_DEBATE is legal only once every department analysis is present and the
     * debate has not already been held — the existing {@code BoardroomDebate}
     * precondition (it reasons over the four completed analyses).
     */
    public boolean debatePreconditionsMet() {
        return !debateStarted && completedAgents.containsAll(ALL_AGENTS);
    }

    /**
     * COMPLETE_ANALYSIS is legal only once the debate has completed — the existing
     * finalize semantics (finalization follows the boardroom debate).
     */
    public boolean completionAllowed() {
        return debateComplete;
    }

    // ---- Builder ------------------------------------------------------------

    public static Builder builder(Long startupId) {
        return new Builder(startupId);
    }

    public static final class Builder {
        private final Long startupId;
        private SimulationPhase currentPhase = SimulationPhase.ANALYSIS;
        private final List<AgentType> completedAgents = new ArrayList<>();
        private final List<AgentType> pendingAgents = new ArrayList<>();
        private final List<CompletedAnalysis> recentAnalyses = new ArrayList<>();
        private final List<AgentMessage> recentMessages = new ArrayList<>();
        private String relevantMemory = "";
        private boolean debateStarted;
        private boolean debateComplete;
        private String executionStatus = "NOT_STARTED";
        private final List<OrchestrationDecision> previousDecisions = new ArrayList<>();

        private Builder(Long startupId) {
            this.startupId = startupId;
        }

        public Builder currentPhase(SimulationPhase phase) {
            this.currentPhase = phase;
            return this;
        }

        public Builder completed(AgentType... agents) {
            this.completedAgents.addAll(List.of(agents));
            return this;
        }

        public Builder completed(List<AgentType> agents) {
            if (agents != null) {
                this.completedAgents.addAll(agents);
            }
            return this;
        }

        public Builder pending(AgentType... agents) {
            this.pendingAgents.addAll(List.of(agents));
            return this;
        }

        public Builder pending(List<AgentType> agents) {
            if (agents != null) {
                this.pendingAgents.addAll(agents);
            }
            return this;
        }

        public Builder analysis(AgentType agent, String headline) {
            this.recentAnalyses.add(new CompletedAnalysis(agent, headline));
            return this;
        }

        public Builder messages(List<AgentMessage> messages) {
            if (messages != null) {
                this.recentMessages.addAll(messages);
            }
            return this;
        }

        public Builder relevantMemory(String block) {
            this.relevantMemory = block;
            return this;
        }

        public Builder debateStarted(boolean v) {
            this.debateStarted = v;
            return this;
        }

        public Builder debateComplete(boolean v) {
            this.debateComplete = v;
            return this;
        }

        public Builder executionStatus(String s) {
            this.executionStatus = s;
            return this;
        }

        public Builder previousDecisions(List<OrchestrationDecision> decisions) {
            if (decisions != null) {
                this.previousDecisions.addAll(decisions);
            }
            return this;
        }

        public OrchestrationStateSnapshot build() {
            return new OrchestrationStateSnapshot(startupId, currentPhase, completedAgents,
                    pendingAgents, recentAnalyses, recentMessages, relevantMemory,
                    debateStarted, debateComplete, executionStatus, previousDecisions);
        }
    }
}
