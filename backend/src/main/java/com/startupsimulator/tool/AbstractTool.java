package com.startupsimulator.tool;

import com.startupsimulator.agent.StartupContext;

/**
 * Template base for deterministic tools. It owns the mechanics every tool shares
 * so concrete tools contain only their math:
 * <ul>
 *   <li>wraps the request's {@link ToolArguments} and times the call;</li>
 *   <li>delegates to {@link #run(ToolArguments, StartupContext)} for the typed output;</li>
 *   <li>turns a controlled {@link ToolValidationException} into a FAILURE result
 *       carrying its machine-readable code; and</li>
 *   <li>turns any unexpected {@link RuntimeException} into a generic
 *       {@code EXECUTION_ERROR} failure — never leaking a stack trace to the agent.</li>
 * </ul>
 *
 * <p>This is an inner guard; {@link ToolExecutionService} wraps the same call in
 * an outer guard, so a tool failure can never crash the simulation (defense in
 * depth). Concrete tools throw {@link ToolValidationException} with
 * {@code OUT_OF_RANGE} (via {@link #inRange}) for domain-range violations.
 */
public abstract class AbstractTool implements Tool {

    @Override
    public final ToolResult execute(ToolRequest request, StartupContext context) {
        long start = System.nanoTime();
        try {
            Object output = run(request.arguments(), context);
            long millis = elapsedMillis(start);
            return ToolResult.success(name(), output, request.requestId(), millis);
        } catch (ToolValidationException e) {
            return ToolResult.failure(name(), e.getCode(), e.getMessage(),
                    request.requestId(), elapsedMillis(start));
        } catch (RuntimeException e) {
            // Controlled: swallow the exception, expose only a safe, generic message.
            return ToolResult.failure(name(), "EXECUTION_ERROR",
                    "The tool failed to complete due to an internal error.",
                    request.requestId(), elapsedMillis(start));
        }
    }

    /**
     * Perform the tool's deterministic calculation. Implementations read typed
     * values from {@code args} (which raise {@link ToolValidationException} on
     * bad input) and return a typed {@code Output} record.
     */
    protected abstract Object run(ToolArguments args, StartupContext context);

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    // ---- shared validation helpers ------------------------------------------

    /** Inclusive range check; raises a controlled {@code OUT_OF_RANGE} failure otherwise. */
    protected static double inRange(String key, double value, double min, double max) {
        if (value < min || value > max) {
            throw new ToolValidationException("OUT_OF_RANGE",
                    "Argument '" + key + "' must be between " + min + " and " + max + ".");
        }
        return value;
    }

    /** Require a strictly-positive value (useful to pre-empt divide-by-zero). */
    protected static double requirePositive(String key, double value) {
        if (value <= 0) {
            throw new ToolValidationException("OUT_OF_RANGE",
                    "Argument '" + key + "' must be greater than zero.");
        }
        return value;
    }

    /** Require a non-negative value. */
    protected static double requireNonNegative(String key, double value) {
        if (value < 0) {
            throw new ToolValidationException("OUT_OF_RANGE",
                    "Argument '" + key + "' must not be negative.");
        }
        return value;
    }
}
