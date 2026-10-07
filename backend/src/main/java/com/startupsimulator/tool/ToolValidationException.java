package com.startupsimulator.tool;

/**
 * Thrown by a {@link Tool} (or by {@link ToolArguments}) when a request's
 * arguments are missing, mistyped, or out of range. It is a <em>controlled</em>
 * failure: the execution boundary ({@link AbstractTool}/{@link ToolExecutionService})
 * catches it and converts it into a {@link ToolResult} with status
 * {@link ToolStatus#FAILURE} — it never escapes to crash the simulation.
 *
 * <p>The {@link #getCode() code} is a short, stable, machine-readable token
 * (e.g. {@code MISSING_ARGUMENT}, {@code INVALID_TYPE}, {@code OUT_OF_RANGE})
 * suitable for programmatic handling by a future agent layer.
 */
public class ToolValidationException extends RuntimeException {

    private final String code;

    public ToolValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
