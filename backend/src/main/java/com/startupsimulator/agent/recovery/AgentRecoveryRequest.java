package com.startupsimulator.agent.recovery;

import com.startupsimulator.model.enums.AgentType;

/**
 * Structured request for agent-level failure recovery adaptation.
 */
public record AgentRecoveryRequest(
        Long startupId,
        AgentType failedAgent,
        String taskOrAction,
        String failureCategory,
        String failureReason,
        String previousContext
) {}
