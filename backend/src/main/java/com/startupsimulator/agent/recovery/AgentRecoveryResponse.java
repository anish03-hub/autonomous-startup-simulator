package com.startupsimulator.agent.recovery;

import com.startupsimulator.model.enums.AgentType;

/**
 * Structured LLM recovery decision detailing the chosen strategy, validated recipient if handoff, and recovery plan.
 */
public record AgentRecoveryResponse(
        AgentRecoveryStrategy strategy,
        AgentType targetAgent,
        String handoffReason,
        String handoffPayload,
        String recoveryPlan
) {}
