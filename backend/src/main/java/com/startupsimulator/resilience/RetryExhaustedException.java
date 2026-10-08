package com.startupsimulator.resilience;

import com.startupsimulator.agent.CeoAnalysisException;

/**
 * Raised when an LLM operation exhausts all configured retry attempts.
 * Inherits from {@link CeoAnalysisException} to seamlessly integrate with
 * explicit REAL-mode failure boundaries.
 */
public class RetryExhaustedException extends CeoAnalysisException {

    private final String operationName;
    private final int attemptsMade;
    private final boolean retried;

    public RetryExhaustedException(String operationName, int attemptsMade, Throwable cause) {
        super(String.format("LLM operation '%s' exhausted after %d attempt(s): %s",
                operationName != null ? operationName : "LLM_OPERATION",
                attemptsMade,
                cause != null ? cause.getMessage() : "unknown failure"), cause);
        this.operationName = operationName != null ? operationName : "LLM_OPERATION";
        this.attemptsMade = attemptsMade;
        this.retried = attemptsMade > 1;
    }

    public String getOperationName() {
        return operationName;
    }

    public int getAttemptsMade() {
        return attemptsMade;
    }

    public boolean isRetried() {
        return retried;
    }

    public boolean retriesAttempted() {
        return retried;
    }
}
