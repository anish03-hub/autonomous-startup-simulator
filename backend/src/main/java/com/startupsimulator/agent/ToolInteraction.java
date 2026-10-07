package com.startupsimulator.agent;

import com.startupsimulator.tool.ToolResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 4B: a single turn of tool interaction recorded during one agent
 * reasoning invocation, so each request/result pair is individually
 * distinguishable (section F "tool requests made" + section G "tool results
 * received" of the prompt, and the correlation story in §17). An interaction is
 * either:
 *
 * <ul>
 *   <li><b>executed</b> — the model asked for {@code toolName} with
 *       {@code arguments}; {@link #result()} holds the <em>actual</em>
 *       {@link ToolResult} the {@code ToolExecutionService} returned (success or
 *       failure). {@link #malformedNote()} is null;</li>
 *   <li><b>malformed</b> — the model's step could not be acted on (no action, a
 *       USE_TOOL with no tool name, an unknown action). {@link #result()} is null
 *       and {@link #malformedNote()} carries the agent-facing explanation that is
 *       fed back so the model can correct itself.</li>
 * </ul>
 *
 * <p>No tool ever mutates an analysis DTO through this type — it is a read-only
 * record of what happened, consumed only to build the next prompt and the final
 * {@link ToolAssistedOutcome}.
 */
public record ToolInteraction(
        int index,
        String toolName,
        String reason,
        Map<String, Object> arguments,
        ToolResult result,
        String malformedNote
) {

    /** True when this interaction is a controlled "your step was invalid" note. */
    public boolean isMalformed() {
        return result == null;
    }

    /** True when a real tool ran and returned a successful result. */
    public boolean succeeded() {
        return result != null && result.isSuccess();
    }

    static ToolInteraction executed(int index, AgentToolStep step, ToolResult result) {
        Map<String, Object> args = step.arguments() == null
                ? Map.of() : new LinkedHashMap<>(step.arguments());
        return new ToolInteraction(index, step.trimmedToolName(), step.reason(), args, result, null);
    }

    static ToolInteraction malformed(int index, String note) {
        return new ToolInteraction(index, null, null, Map.of(), null, note);
    }
}
