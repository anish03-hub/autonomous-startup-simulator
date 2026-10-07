package com.startupsimulator.agent;

import com.startupsimulator.model.enums.AgentType;

import java.util.List;

/**
 * A department agent. Each implementation encapsulates the responsibilities of
 * one department and contributes to the shared {@link StartupContext}. In
 * Phase 1 the reasoning is deterministic; the {@link LLMService} seam lets a
 * real model take over message phrasing later.
 */
public interface StartupAgent {

    AgentType type();

    /** One-line objective shown at the top of the department view. */
    String initialObjective(StartupContext ctx);

    /** Ordered subtasks this agent works through (drives the task checklist). */
    List<String> plannedSubtasks();

    /**
     * Run this department's analysis, mutating the shared context (adding
     * features, filling in the plan/budget, updating narrative fields) and
     * returning a human-readable status message for the department feed.
     */
    String analyze(StartupContext ctx);

    /** This department's opening position when the boardroom debate starts. */
    String debateStatement(StartupContext ctx);
}
