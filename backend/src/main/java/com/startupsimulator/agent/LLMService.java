package com.startupsimulator.agent;

/**
 * Provider-independent abstraction over a Large Language Model. Phase 1 shipped
 * a deterministic {@link MockLLMService}; Phase 2A adds a real
 * {@link OpenAiLLMService}. The rest of the application depends on this
 * interface only — never on a concrete provider SDK — so Anthropic, Gemini,
 * Bedrock, etc. can be added later by implementing this interface, with no
 * agent or orchestrator changes.
 */
public interface LLMService {

    /**
     * Lightweight phrasing pass used by the mock department agents. In every
     * implementation this returns the caller's draft unchanged (identity
     * passthrough) so it costs nothing and stays deterministic; the seam exists
     * so a real provider could reword department messages in a later phase
     * without touching agent logic.
     */
    String complete(String system, String user);

    /**
     * Free-form text completion from the model. A real provider sends the
     * prompt and returns the model's answer; the mock returns the prompt.
     */
    String generate(String system, String user);

    /**
     * Structured completion: instruct the model to return JSON and parse it
     * into {@code type}. Real providers request JSON output and deserialize;
     * the mock does not support arbitrary structured generation and throws
     * {@link UnsupportedOperationException} (tests use a fake, and the CEO agent
     * only calls this when a real provider is active).
     *
     * @throws CeoAnalysisException on transport, provider, or parse failure
     */
    <T> T generateStructured(String system, String user, Class<T> type);

    /** Identifier of the active provider, e.g. {@code "mock"} or {@code "openai"}. */
    String provider();

    /**
     * Whether this provider can actually reach a real model (has credentials).
     * The mock is never "real"; callers use this to decide between a live call
     * and a deterministic path.
     */
    default boolean isRealProvider() {
        return false;
    }
}
