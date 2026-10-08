package com.startupsimulator.agent.recovery;

import com.startupsimulator.model.enums.AgentType;

/**
 * Result of an agent failure recovery attempt by AgentRecoveryEngine.
 */
public record AgentRecoveryOutcome(
        boolean success,
        boolean handoff,
        AgentType targetAgent,
        String handoffReason,
        String handoffPayload,
        String recoveryPlan,
        String error
) {
    public static AgentRecoveryOutcome failed(String error) {
        return new AgentRecoveryOutcome(false, false, null, null, null, null, error);
    }

    public static AgentRecoveryOutcome handoff(AgentType targetAgent, String handoffReason, String handoffPayload) {
        return new AgentRecoveryOutcome(true, true, targetAgent, handoffReason, handoffPayload, null, null);
    }

    public static AgentRecoveryOutcome recoveredSelf(String recoveryPlan) {
        return new AgentRecoveryOutcome(true, false, null, null, null, recoveryPlan, null);
    }
}
