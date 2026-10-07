package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.model.enums.AgentType;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * CA3 Phase 6A: the authoritative validation pipeline for a proposed
 * {@link OrchestrationDecision} (requirement 11). It is pure, deterministic Java
 * and the single place that decides whether an LLM-proposed decision is accepted
 * — it never <em>produces</em> a decision, so it is not a disguised deterministic
 * orchestrator.
 *
 * <p>Stages, in order; the first failure wins and is reported explicitly
 * (requirement 11/12 — no silent fallback, no coercion):
 * <ol>
 *   <li><b>schema</b> — decision present, action string resolvable to a known
 *       {@link OrchestrationAction};</li>
 *   <li><b>target</b> — RUN_AGENT must carry a {@code targetAgent} that resolves
 *       <em>strictly</em> (exact enum-name, case-insensitive) to a real
 *       {@link AgentType}; "ACCOUNTANT"/"developer-ish" are rejected, never
 *       mapped;</li>
 *   <li><b>current-state legality</b> — the action must be legal for the supplied
 *       {@link OrchestrationStateSnapshot} (RUN_AGENT target among the pending
 *       departments; START_DEBATE only when debate preconditions hold;
 *       COMPLETE_ANALYSIS only when completion is allowed).</li>
 * </ol>
 */
@Component
public class OrchestrationDecisionValidator {

    public OrchestrationValidation validate(OrchestrationDecision decision,
                                            OrchestrationStateSnapshot snapshot) {
        // 1. Schema.
        if (decision == null) {
            return OrchestrationValidation.rejected("No decision was produced (null response).");
        }
        if (snapshot == null) {
            return OrchestrationValidation.rejected("No orchestration state snapshot was supplied.");
        }
        Optional<OrchestrationAction> resolved = decision.resolvedAction();
        if (resolved.isEmpty()) {
            return OrchestrationValidation.rejected(
                    "Unrecognised or missing action '" + decision.action() + "'. Legal actions: "
                            + "RUN_AGENT, START_DEBATE, COMPLETE_ANALYSIS.");
        }
        OrchestrationAction action = resolved.get();

        // 2 + 3. Action-specific target + current-state legality.
        return switch (action) {
            case RUN_AGENT -> validateRunAgent(decision, snapshot);
            case START_DEBATE -> validateStartDebate(snapshot);
            case COMPLETE_ANALYSIS -> validateCompleteAnalysis(snapshot);
        };
    }

    private OrchestrationValidation validateRunAgent(OrchestrationDecision decision,
                                                     OrchestrationStateSnapshot snapshot) {
        String raw = decision.trimmedTarget();
        if (raw == null || raw.isBlank()) {
            return OrchestrationValidation.rejected("RUN_AGENT requires a targetAgent, but none was supplied.");
        }
        Optional<AgentType> target = resolveAgentStrict(raw);
        if (target.isEmpty()) {
            return OrchestrationValidation.rejected(
                    "RUN_AGENT target '" + raw + "' is not a valid agent. Valid agents: "
                            + "CEO, DEVELOPMENT, MARKETING, FINANCE.");
        }
        AgentType agent = target.get();
        if (!snapshot.legalRunAgentTargets().contains(agent)) {
            return OrchestrationValidation.rejected(
                    "RUN_AGENT target " + agent.name() + " is not legal in the current state "
                            + "(it is not among the pending agents " + snapshot.legalRunAgentTargets() + ").");
        }
        return OrchestrationValidation.accepted(OrchestrationAction.RUN_AGENT, agent);
    }

    private OrchestrationValidation validateStartDebate(OrchestrationStateSnapshot snapshot) {
        if (!snapshot.debatePreconditionsMet()) {
            return OrchestrationValidation.rejected(
                    "START_DEBATE is not legal in the current state: it requires all department analyses "
                            + "(CEO, DEVELOPMENT, MARKETING, FINANCE) complete and the debate not already held. "
                            + "Completed so far: " + snapshot.completedAgents() + ".");
        }
        return OrchestrationValidation.accepted(OrchestrationAction.START_DEBATE, null);
    }

    private OrchestrationValidation validateCompleteAnalysis(OrchestrationStateSnapshot snapshot) {
        if (!snapshot.completionAllowed()) {
            return OrchestrationValidation.rejected(
                    "COMPLETE_ANALYSIS is not legal in the current state: the boardroom debate has not "
                            + "yet completed.");
        }
        return OrchestrationValidation.accepted(OrchestrationAction.COMPLETE_ANALYSIS, null);
    }

    /**
     * Strict agent resolution: exact {@link AgentType} enum-name match, trimmed and
     * case-insensitive. Deliberately NOT display-name or substring matching — the
     * engine's prompt instructs the model to use the enum names, and anything else
     * is an explicit rejection rather than a guess.
     */
    private Optional<AgentType> resolveAgentStrict(String raw) {
        for (AgentType t : AgentType.values()) {
            if (t.name().equalsIgnoreCase(raw)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
