package com.startupsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider-independent LLM configuration, bound from {@code app.ai.*}. Secrets
 * (the API key) come from environment variables — never hard-coded, never
 * committed. Phase 2A ships the {@code openai} provider; adding Anthropic or
 * Gemini means adding a provider block and an {@code LLMService} implementation,
 * not touching agent code.
 *
 * <pre>
 * app:
 *   ai:
 *     provider: openai        # "mock" (default, offline) or "openai"
 *     openai:
 *       api-key: ${OPENAI_API_KEY}
 *       model:   ${OPENAI_MODEL:gpt-4o-mini}
 *       base-url:${OPENAI_BASE_URL:https://api.openai.com/v1}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.ai")
public class LlmProperties {

    /** Active provider: {@code mock} (default) or {@code openai}. */
    private String provider = "mock";

    /**
     * Execution mode, independent of provider availability. Default
     * {@link AiExecutionMode#SCRIPTED_DEMO} keeps un-configured environments and
     * all tests offline-safe. Set {@code REAL} to require genuine LLM reasoning.
     */
    private AiExecutionMode mode = AiExecutionMode.SCRIPTED_DEMO;

    private final OpenAi openai = new OpenAi();

    /** Overall request timeout for a single LLM call. */
    private int requestTimeoutSeconds = 45;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public AiExecutionMode getMode() {
        return mode;
    }

    public void setMode(AiExecutionMode mode) {
        this.mode = mode == null ? AiExecutionMode.SCRIPTED_DEMO : mode;
    }

    /** True when the operator has requested genuine LLM reasoning (REAL mode). */
    public boolean isRealMode() {
        return mode == AiExecutionMode.REAL;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    /** True only when a non-blank API key has been supplied for OpenAI. */
    public boolean isOpenAiConfigured() {
        return "openai".equalsIgnoreCase(provider)
                && openai.getApiKey() != null && !openai.getApiKey().isBlank();
    }

    public static class OpenAi {
        private String apiKey;
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
