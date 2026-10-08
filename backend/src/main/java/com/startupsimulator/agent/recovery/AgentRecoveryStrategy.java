package com.startupsimulator.agent.recovery;

/**
 * Strategy chosen during agent-level failure adaptation.
 */
public enum AgentRecoveryStrategy {
    RECOVER_SELF,
    HANDOFF,
    ABORT
}
