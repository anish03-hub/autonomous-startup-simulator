package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.model.enums.AgentType;

/**
 * CA3 Phase 6A: the result of one orchestration-decision invocation
 * ({@link OrchestrationDecisionEngine#decideNext}). It records what the engine
 * concluded without ever having let Java invent the decision in REAL mode.
 *
 * <p>Status values:
 * <ul>
 *   <li>{@link #ACCEPTED} — a decision (LLM-proposed in REAL mode, deterministic
 *       in SCRIPTED_DEMO) passed validation; {@link #resolvedAgent} is set for a
 *       RUN_AGENT decision.</li>
 *   <li>{@link #REJECTED} — a decision was produced but failed validation
 *       (unknown action, invalid agent, illegal for the current state). No action
 *       is taken and nothing is coerced.</li>
 *   <li>{@link #LLM_FAILURE} — REAL mode and the LLM call/transport/parse failed.
 *       Explicit failure, no deterministic substitution (requirement 12).</li>
 *   <li>{@link #NO_REAL_PROVIDER} — REAL mode requested but no real provider is
 *       available; nothing is fabricated.</li>
 * </ul>
 */
public record OrchestrationDecisionOutcome(
        String status,
        OrchestrationDecision decision,
        OrchestrationAction action,
        AgentType resolvedAgent,
        String detail,
        boolean usedRealLlm
) {

    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String LLM_FAILURE = "LLM_FAILURE";
    public static final String NO_REAL_PROVIDER = "NO_REAL_PROVIDER";

    public boolean isAccepted() {
        return ACCEPTED.equals(status);
    }

    /** True for anything that did not yield an accepted decision. */
    public boolean failed() {
        return !isAccepted();
    }

    static OrchestrationDecisionOutcome accepted(OrchestrationDecision decision, OrchestrationAction action,
                                                 AgentType resolvedAgent, String detail, boolean usedRealLlm) {
        return new OrchestrationDecisionOutcome(ACCEPTED, decision, action, resolvedAgent, detail, usedRealLlm);
    }

    static OrchestrationDecisionOutcome rejected(OrchestrationDecision decision, String detail, boolean usedRealLlm) {
        return new OrchestrationDecisionOutcome(REJECTED, decision, null, null, detail, usedRealLlm);
    }

    static OrchestrationDecisionOutcome llmFailure(String detail) {
        return new OrchestrationDecisionOutcome(LLM_FAILURE, null, null, null, detail, true);
    }

    static OrchestrationDecisionOutcome noRealProvider(String detail) {
        return new OrchestrationDecisionOutcome(NO_REAL_PROVIDER, null, null, null, detail, false);
    }
}
