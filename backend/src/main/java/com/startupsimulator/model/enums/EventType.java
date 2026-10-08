package com.startupsimulator.model.enums;

/**
 * All event types emitted by the simulation. These are persisted to the
 * {@code startup_events} table and streamed to the frontend to drive the
 * living-office UI.
 */
public enum EventType {
    STARTUP_CREATED,
    AGENT_INITIALIZED,
    TASK_CREATED,
    AGENT_STARTED_WORK,
    AGENT_PROGRESS_UPDATED,
    AGENT_MESSAGE_CREATED,
    DEBATE_STARTED,
    AGENT_JOINED_DEBATE,
    AGENT_RESPONDED,
    DECISION_PROPOSED,
    DECISION_APPROVED,
    MVP_UPDATED,
    BUDGET_UPDATED,
    PHASE_COMPLETED,
    BLUEPRINT_GENERATED,

    // ---- Phase 2A: real LLM-powered CEO agent -------------------------------
    CEO_ANALYSIS_STARTED,
    CEO_ANALYSIS_COMPLETED,
    CEO_ANALYSIS_FAILED,
    LLM_REQUEST_STARTED,
    LLM_RESPONSE_RECEIVED,

    // ---- Phase 2B: real LLM-powered department agents -----------------------
    AGENT_ANALYSIS_STARTED,
    AGENT_ANALYSIS_COMPLETED,
    AGENT_ANALYSIS_FAILED,

    // ---- Phase 2C: autonomous, LLM-powered boardroom debate -----------------
    DEBATE_ROUND_STARTED,
    AGENT_DEBATE_STARTED,
    AGENT_DEBATE_MESSAGE,
    AGENT_DEBATE_COMPLETED,
    DEBATE_ROUND_COMPLETED,
    CEO_SYNTHESIS_STARTED,
    CEO_SYNTHESIS_COMPLETED,
    DEBATE_COMPLETED,
    DECISION_STARTED,
    DECISION_COMPLETED,

    // ---- Phase 3: real agent-to-agent addressable messaging -----------------
    // Distinct from AGENT_MESSAGE_CREATED (a feed status line): these reflect a
    // real addressed message being delivered, retrieved by its recipient, or
    // rejected by recipient validation.
    AGENT_MESSAGE_SENT,
    AGENT_MESSAGE_RECEIVED,
    AGENT_MESSAGE_FAILED,

    // ---- Phase 2F: Autonomous Startup Execution ----
    EXECUTION_STARTED,
    TASK_ASSIGNED,
    TASK_STARTED,
    TASK_PROGRESS,
    TASK_COMPLETED,
    TASK_BLOCKED,
    EMPLOYEE_STARTED_WORK,
    EMPLOYEE_FINISHED_WORK,
    METRIC_CHANGED,
    CEO_INTERVENTION_REQUIRED,
    EXECUTION_PAUSED,
    EXECUTION_RESUMED,
    EXECUTION_COMPLETED,

    // ---- Phase 4A: agent tool infrastructure --------------------------------
    // Emitted by ToolExecutionService ONLY when a resolved tool actually runs:
    // STARTED once execution begins, then exactly one of SUCCEEDED / FAILED
    // reflecting the real outcome. Pre-execution validation failures (null
    // request, unknown tool) emit no event.
    TOOL_EXECUTION_STARTED,
    TOOL_EXECUTION_SUCCEEDED,
    TOOL_EXECUTION_FAILED,

    // ---- Phase 5A: persistent agent memory infrastructure -------------------
    // MEMORY_CREATED is emitted by MemoryService ONLY after a memory row has
    // actually been persisted (it carries the DB-assigned id). A rejected,
    // duplicate, or un-persisted candidate emits no MEMORY_CREATED.
    // MEMORY_RETRIEVAL_FAILED reflects a controlled, invalid retrieval request
    // (e.g. a null startup/agent) — the lookup returns empty, never throws.
    MEMORY_CREATED,
    MEMORY_RETRIEVAL_FAILED,

    // ---- Phase 5B: agents retrieve, reason with, and create memory ----------
    // MEMORY_RETRIEVED is emitted once per memory-aware reasoning turn, after the
    // relevant persistent memories for (startup, agent) have been selected and
    // bounded for injection into the agent's prompt. It carries the agent and the
    // number of memories that entered the reasoning context (0 is valid and still
    // demonstrates that a retrieval occurred).
    MEMORY_RETRIEVED,

    // ---- Phase 6A: dynamic orchestration decision engine --------------------
    // The orchestration decision engine asks the LLM "given the current state,
    // what should happen next?". REQUESTED is emitted once a decision is sought
    // (carrying a bounded state summary); then exactly one of ACCEPTED (the
    // proposed, validated decision — action, selected agent, why) or REJECTED (an
    // invalid/illegal/failed proposal, with the explicit cause). There is no
    // hidden deterministic fallback in REAL mode — a rejection stays a rejection.
    ORCHESTRATION_DECISION_REQUESTED,
    ORCHESTRATION_DECISION_ACCEPTED,
    ORCHESTRATION_DECISION_REJECTED,

    // ---- Phase 6B: dynamic orchestration runtime ----------------------------
    // The runtime now EXECUTES accepted decisions in a bounded dynamic loop
    // (never a fixed CEO→DEV→MKT→FIN order in REAL mode). ACTION_STARTED is
    // emitted immediately before an accepted action runs (RUN_AGENT /
    // START_DEBATE / COMPLETE_ANALYSIS) and ACTION_COMPLETED immediately after it
    // has actually changed state. RUNTIME_TERMINATED is emitted exactly once when
    // the loop stops abnormally — a rejected/failed decision, a non-progressing
    // proposal, or MAX_ORCHESTRATION_STEPS exhaustion — carrying the explicit
    // cause. There is NO silent fixed-order fallback: termination stays terminal.
    ORCHESTRATION_ACTION_STARTED,
    ORCHESTRATION_ACTION_COMPLETED,
    ORCHESTRATION_RUNTIME_TERMINATED,

    // ---- Phase 7: adaptive execution & replanning ----------------------------
    REPLAN_REQUESTED,
    REPLAN_PROPOSED,
    REPLAN_ACCEPTED,
    REPLAN_REJECTED,
    TASK_MODIFIED,
    TASK_DEFERRED,
    TASK_CANCELLED,

    // ---- Phase 8A: retry + backoff resilience infrastructure ----------------
    LLM_RETRY_ATTEMPTED,
    LLM_RETRY_EXHAUSTED,

    // ---- Phase 8B / Section A: Agent recovery & failure adaptation -----------
    AGENT_RECOVERY_REQUESTED,
    AGENT_RECOVERY_STARTED,
    AGENT_RECOVERY_HANDOFF,
    AGENT_RECOVERY_SUCCEEDED,
    AGENT_RECOVERY_FAILED
}


