package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.model.enums.AgentType;

/**
 * CA3 Phase 6A: the typed outcome of validating one proposed
 * {@link OrchestrationDecision} against an {@link OrchestrationStateSnapshot}.
 * "LLM proposes, Java validates" — this is the Java verdict.
 *
 * <p>On {@link #valid} true the decision is accepted; for a RUN_AGENT decision
 * {@link #resolvedAgent} is the real {@link AgentType} the raw string resolved to
 * (null for the agent-less actions). On rejection {@link #valid} is false and
 * {@link #reason} carries the explicit, human-readable cause — there is never a
 * silent coercion to a legal action.
 */
public record OrchestrationValidation(
        boolean valid,
        OrchestrationAction action,
        AgentType resolvedAgent,
        String reason
) {

    static OrchestrationValidation accepted(OrchestrationAction action, AgentType resolvedAgent) {
        return new OrchestrationValidation(true, action, resolvedAgent, null);
    }

    static OrchestrationValidation rejected(String reason) {
        return new OrchestrationValidation(false, null, null, reason);
    }
}
