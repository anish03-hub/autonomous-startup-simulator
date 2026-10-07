package com.startupsimulator.tool;

import com.startupsimulator.agent.StartupContext;

/**
 * A deterministic, self-contained capability an agent can invoke.
 *
 * <p>A tool exposes a unique, machine-readable {@link #name()} and an
 * LLM-readable {@link #description()}, declares its typed input via
 * {@link #schema()}, and performs a bounded, side-effect-free calculation in
 * {@link #execute(ToolRequest, StartupContext)}, returning a structured
 * {@link ToolResult}.
 *
 * <p>Phase 4A contract (enforced by convention + {@link AbstractTool}):
 * a tool MUST NOT call the LLM, MUST NOT secretly invoke another agent or tool,
 * MUST NOT mutate unrelated agent/simulation state, and MUST NOT touch the
 * filesystem, network, reflection, or a database. Tools are pure functions of
 * their arguments (plus read-only {@link StartupContext}).
 *
 * <p>Most tools should extend {@link AbstractTool}, which supplies the timing,
 * argument-wrapping, and controlled-failure handling so each concrete tool only
 * writes its math.
 */
public interface Tool {

    /** Unique, machine-readable identifier (e.g. {@code financial_calculator}). */
    String name();

    /** Human-/LLM-readable explanation of what the tool does. */
    String description();

    /** Structured input description for the eventual Phase 4B LLM layer. */
    ToolSchema schema();

    /** Execute against validated input, returning a structured result (never throwing to the caller). */
    ToolResult execute(ToolRequest request, StartupContext context);
}
