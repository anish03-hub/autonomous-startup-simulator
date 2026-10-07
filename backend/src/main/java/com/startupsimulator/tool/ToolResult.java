package com.startupsimulator.tool;

/**
 * The immutable outcome of a tool execution attempt.
 *
 * <p>Deliberately split so a Phase 4B agent can branch on {@link #status()}
 * without parsing prose:
 * <ul>
 *   <li>{@link ToolStatus#SUCCESS}: {@link #result()} holds the tool's typed
 *       output payload; {@code errorCode}/{@code errorMessage} are null.</li>
 *   <li>{@link ToolStatus#FAILURE}: {@code errorCode} is a short machine-readable
 *       token and {@code errorMessage} is a safe, concise explanation. The payload
 *       is null, and crucially <em>no stack trace is ever exposed</em> here.</li>
 * </ul>
 *
 * <p>{@code toolName} and {@code requestId} echo the request for traceability, and
 * {@code durationMillis} is lightweight execution metadata. The {@code result} is
 * typed as {@link Object} only to stay tool-agnostic at this boundary — each tool
 * populates it with its own typed {@code Output} record (see the {@code impl} tools).
 *
 * @param toolName       the tool this result came from
 * @param status         SUCCESS or FAILURE
 * @param result         typed output payload on success, else null
 * @param errorCode      machine-readable failure code on failure, else null
 * @param errorMessage   safe, agent-facing failure message on failure, else null
 * @param requestId      correlation id echoed from the {@link ToolRequest}
 * @param durationMillis wall-clock execution time, for traceability
 */
public record ToolResult(
        String toolName,
        ToolStatus status,
        Object result,
        String errorCode,
        String errorMessage,
        String requestId,
        long durationMillis
) {
    public boolean isSuccess() {
        return status == ToolStatus.SUCCESS;
    }

    public boolean isFailure() {
        return status == ToolStatus.FAILURE;
    }

    public static ToolResult success(String toolName, Object result,
                                     String requestId, long durationMillis) {
        return new ToolResult(toolName, ToolStatus.SUCCESS, result, null, null,
                requestId, durationMillis);
    }

    public static ToolResult failure(String toolName, String errorCode, String errorMessage,
                                     String requestId, long durationMillis) {
        return new ToolResult(toolName, ToolStatus.FAILURE, null, errorCode, errorMessage,
                requestId, durationMillis);
    }
}
