package com.startupsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider-independent LLM configuration, bound from {@code app.ai.*}. Secrets
 * (the API key) come from environment variables — never hard-coded, never
 * committed. Ships with {@code justdowork} (Claude Opus 4.8 via API gateway) and
 * {@code openai} providers.
 *
 * <pre>
 * app:
 *   ai:
 *     provider: justdowork    # "justdowork" (default), "openai", or "mock"
 *     justdowork:
 *       api-key: ${JUSTDOWORK_API_KEY}
 *       model:   ${JUSTDOWORK_MODEL:claude-opus-4-8}
 *       base-url:${JUSTDOWORK_BASE_URL:https://api.justwoker.icu/v1}
 *     openai:
 *       api-key: ${OPENAI_API_KEY}
 *       model:   ${OPENAI_MODEL:gpt-4o-mini}
 *       base-url:${OPENAI_BASE_URL:https://api.openai.com/v1}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.ai")
public class LlmProperties {

    /** Active provider: {@code justdowork} (production default bound via application.yml). */
    private String provider = "justdowork";

    /**
     * Execution mode. Spring binds this from {@code app.ai.mode} (default {@code REAL} in application.yml).
     */
    private AiExecutionMode mode = AiExecutionMode.SCRIPTED_DEMO;

    private final JustDoWork justdowork = new JustDoWork();

    private final OpenAi openai = new OpenAi();

    private final Gemini gemini = new Gemini();

    private final Resilience resilience = new Resilience();

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

    public JustDoWork getJustdowork() {
        return justdowork;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public Resilience getResilience() {
        return resilience;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    /** True only when a non-blank API key has been supplied for JustDoWork. */
    public boolean isJustDoWorkConfigured() {
        return "justdowork".equalsIgnoreCase(provider)
                && justdowork.getApiKey() != null && !justdowork.getApiKey().isBlank();
    }

    /** True only when a non-blank API key has been supplied for OpenAI. */
    public boolean isOpenAiConfigured() {
        return "openai".equalsIgnoreCase(provider)
                && openai.getApiKey() != null && !openai.getApiKey().isBlank();
    }

    /** True only when a non-blank API key has been supplied for Gemini. */
    public boolean isGeminiConfigured() {
        return "gemini".equalsIgnoreCase(provider)
                && gemini.getApiKey() != null && !gemini.getApiKey().isBlank();
    }

    public static class Resilience {
        private int maxAttempts = 3;
        private long initialBackoffMs = 250;
        private double backoffMultiplier = 2.0;
        private long maxBackoffMs = 2000;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getInitialBackoffMs() { return initialBackoffMs; }
        public void setInitialBackoffMs(long initialBackoffMs) { this.initialBackoffMs = initialBackoffMs; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        public long getMaxBackoffMs() { return maxBackoffMs; }
        public void setMaxBackoffMs(long maxBackoffMs) { this.maxBackoffMs = maxBackoffMs; }

        public com.startupsimulator.resilience.RetryPolicy toRetryPolicy() {
            return new com.startupsimulator.resilience.RetryPolicy(maxAttempts, initialBackoffMs, backoffMultiplier, maxBackoffMs);
        }
    }

    public static class Gemini {
        private String apiKey;
        private String model = "gemini-3.8-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

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

    public static class JustDoWork {
        private String apiKey;
        private String model = "claude-opus-4-8";
        private String baseUrl = "https://api.justwoker.icu/v1";

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

