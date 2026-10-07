package com.startupsimulator.agent;

/**
 * Raised when a real LLM CEO analysis cannot be produced or validated — missing
 * configuration, transport failure, provider error, or a malformed/invalid
 * structured response. Callers catch this to emit {@code CEO_ANALYSIS_FAILED},
 * surface a failure state, and fall back to a deterministic analysis so the
 * simulation never crashes on a single LLM failure.
 */
public class CeoAnalysisException extends RuntimeException {

    public CeoAnalysisException(String message) {
        super(message);
    }

    public CeoAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
