package com.startupsimulator.tool;

import java.util.UUID;

/**
 * An immutable request to execute one tool.
 *
 * <p>This is the exact structure a Phase 4B agent layer will produce once it has
 * decided (by its own reasoning — NOT here) that it needs a tool: it names the
 * tool, supplies typed-but-generic {@link ToolArguments}, and identifies who is
 * asking and on behalf of which simulation. {@link ToolExecutionService} is the
 * only thing that acts on it.
 *
 * @param requestId       correlation id for traceability (auto-generated if absent)
 * @param startupId       the simulation this request belongs to
 * @param requestingAgent machine-readable identity of the caller (e.g. an {@code AgentType} name)
 * @param toolName        the unique name of the tool to run
 * @param arguments       the tool's input arguments (never null)
 */
public record ToolRequest(
        String requestId,
        Long startupId,
        String requestingAgent,
        String toolName,
        ToolArguments arguments
) {
    public ToolRequest {
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        if (arguments == null) {
            arguments = ToolArguments.empty();
        }
    }

    /** Convenience factory that fills in a fresh correlation id. */
    public static ToolRequest of(Long startupId, String requestingAgent,
                                 String toolName, ToolArguments arguments) {
        return new ToolRequest(null, startupId, requestingAgent, toolName, arguments);
    }
}
