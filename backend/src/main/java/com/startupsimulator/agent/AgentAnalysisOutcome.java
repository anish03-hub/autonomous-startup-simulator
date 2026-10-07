package com.startupsimulator.agent;

import com.startupsimulator.model.enums.AgentType;

/**
 * Result of a department agent's analysis run (Developer/Marketing/Finance),
 * carrying enough metadata for the orchestrator (and the retry endpoints) to
 * emit the right AGENT_ANALYSIS_* / LLM_* events without re-deriving state.
 *
 * <p>Unlike {@link CeoAnalysisOutcome} this is provider-shape agnostic — each
 * department has its own typed response DTO, but the orchestrator only needs
 * the provider/failure metadata and a headline for the department feed.
 *
 * @param agentType       which department produced it
 * @param provider        provider that produced it: "openai", "mock", or "fallback"
 * @param usedRealLlm     true when a real provider produced the analysis
 * @param failed          true when a real attempt failed and a fallback was used
 * @param error           short, non-sensitive failure description (null on success)
 * @param headlineMessage a one-line summary for the department message feed
 */
public record AgentAnalysisOutcome(
        AgentType agentType,
        String provider,
        boolean usedRealLlm,
        boolean failed,
        String error,
        String headlineMessage
) {
    static AgentAnalysisOutcome success(AgentType agentType, String provider, String headline) {
        return new AgentAnalysisOutcome(agentType, provider, true, false, null, headline);
    }

    static AgentAnalysisOutcome failed(AgentType agentType, String provider, String error, String headline) {
        return new AgentAnalysisOutcome(agentType, provider, false, true, error, headline);
    }

    static AgentAnalysisOutcome mock(AgentType agentType, String headline) {
        return new AgentAnalysisOutcome(agentType, "mock", false, false, null, headline);
    }
}
