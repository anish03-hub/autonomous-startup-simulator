package com.startupsimulator.agent;

/**
 * Result of a CEO analysis run, carrying enough metadata for the orchestrator
 * (and the retry endpoint) to emit the right events without re-deriving state.
 *
 * @param response       the validated/normalised analysis (LLM or fallback)
 * @param provider       provider that produced it: "openai", "mock", or "fallback"
 * @param usedRealLlm    true when a real provider produced the analysis
 * @param failed         true when a real attempt failed and a fallback was used
 * @param error          short, non-sensitive failure description (null on success)
 * @param headlineMessage a one-line summary for the department message feed
 */
public record CeoAnalysisOutcome(
        CeoAnalysisResponse response,
        String provider,
        boolean usedRealLlm,
        boolean failed,
        String error,
        String headlineMessage
) {
    static CeoAnalysisOutcome success(CeoAnalysisResponse response, String provider, String headline) {
        return new CeoAnalysisOutcome(response, provider, true, false, null, headline);
    }

    static CeoAnalysisOutcome failed(CeoAnalysisResponse fallback, String provider, String error, String headline) {
        return new CeoAnalysisOutcome(fallback, provider, false, true, error, headline);
    }

    static CeoAnalysisOutcome mock(CeoAnalysisResponse response, String headline) {
        return new CeoAnalysisOutcome(response, "mock", false, false, null, headline);
    }
}
