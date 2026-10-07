package com.startupsimulator.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Deterministic, offline stand-in for a real LLM. It returns the draft the
 * caller already composed (identity passthrough) so the app runs with ZERO
 * setup and behaves reproducibly — this is the default provider and preserves
 * Phase 1 behavior when no {@code OPENAI_API_KEY} is configured.
 *
 * <p>Registered as the sole {@link LLMService} for every {@code app.ai.provider}
 * value other than {@code openai} (and when the property is missing). A real
 * provider such as {@link OpenAiLLMService} is registered instead only when
 * {@code app.ai.provider=openai}, so exactly one implementation ever exists and
 * no {@code @Primary} tie-break is required.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockLLMService implements LLMService {

    @Override
    public String complete(String system, String user) {
        return user == null ? "" : user.trim();
    }

    @Override
    public String generate(String system, String user) {
        return complete(system, user);
    }

    @Override
    public <T> T generateStructured(String system, String user, Class<T> type) {
        // The mock cannot fabricate an arbitrary typed response. Agents that
        // want structured output check isRealProvider() first and use their own
        // deterministic fallback when the provider is mock.
        throw new UnsupportedOperationException(
                "MockLLMService does not support structured generation; use a real provider or a deterministic fallback.");
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public boolean isRealProvider() {
        return false;
    }
}
