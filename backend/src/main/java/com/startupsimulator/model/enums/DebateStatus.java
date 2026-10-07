package com.startupsimulator.model.enums;

public enum DebateStatus {
    OPEN,
    RESOLVED,
    /** Debate finished, but at least one agent's LLM turn failed and used a fallback (Phase 2C, §17). */
    COMPLETED_WITH_WARNINGS
}
