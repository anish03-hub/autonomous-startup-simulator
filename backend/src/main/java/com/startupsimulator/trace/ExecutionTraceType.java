package com.startupsimulator.trace;

/**
 * Execution trace event types for CA3 Section A queryable execution trace.
 */
public enum ExecutionTraceType {
    AGENT_ACTION,
    AGENT_MESSAGE,
    LLM_REASONING,
    TOOL_CALL,
    TOOL_RESULT,
    MEMORY_RETRIEVAL,
    MEMORY_CREATION,
    ORCHESTRATION_DECISION,
    REPLAN,
    RECOVERY,
    FAILURE,
    HANDOFF,
    TASK_STATE_CHANGE
}
