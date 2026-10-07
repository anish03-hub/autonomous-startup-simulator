package com.startupsimulator.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupsimulator.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-backed {@link LLMService} (Chat Completions API). Registered as the
 * sole {@link LLMService} only when {@code app.ai.provider=openai}; otherwise
 * {@link MockLLMService} is used, so exactly one implementation exists and no
 * {@code @Primary} tie-break is required.
 *
 * <p>The API key is read from configuration (env-driven) and never logged or
 * exposed. Structured calls request {@code response_format=json_object} and the
 * parsed content is deserialized into the requested type. Any transport,
 * provider, or parse problem is surfaced as {@link CeoAnalysisException} so the
 * caller can fail gracefully.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai")
public class OpenAiLLMService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLLMService.class);

    private final LlmProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiLLMService(LlmProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        LlmProperties.OpenAi cfg = props.getOpenai();
        this.restClient = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + safeKey(cfg.getApiKey()))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("OpenAiLLMService initialised (model={}, baseUrl={}, apiKeyPresent={})",
                cfg.getModel(), cfg.getBaseUrl(), cfg.getApiKey() != null && !cfg.getApiKey().isBlank());
    }

    @Override
    public String complete(String system, String user) {
        // Phrasing passthrough — mock department agents call this and must NOT
        // trigger paid API calls (cost control: one LLM call per simulation).
        return user == null ? "" : user.trim();
    }

    @Override
    public String generate(String system, String user) {
        return chat(system, user, false);
    }

    @Override
    public <T> T generateStructured(String system, String user, Class<T> type) {
        String content = chat(system, user, true);
        try {
            return objectMapper.readValue(content, type);
        } catch (Exception e) {
            throw new CeoAnalysisException("Failed to parse structured LLM response as " + type.getSimpleName(), e);
        }
    }

    /** Make a single Chat Completions call and return the assistant message text. */
    private String chat(String system, String user, boolean json) {
        if (props.getOpenai().getApiKey() == null || props.getOpenai().getApiKey().isBlank()) {
            throw new CeoAnalysisException("OpenAI API key is not configured (set OPENAI_API_KEY).");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getOpenai().getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)));
        body.put("temperature", 0.4);
        if (json) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        long started = System.currentTimeMillis();
        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            long latency = System.currentTimeMillis() - started;
            String content = extractContent(raw);
            log.info("LLM call ok (provider=openai, model={}, latencyMs={}, chars={})",
                    props.getOpenai().getModel(), latency, content == null ? 0 : content.length());
            if (content == null || content.isBlank()) {
                throw new CeoAnalysisException("OpenAI returned an empty completion.");
            }
            return content;
        } catch (CeoAnalysisException e) {
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - started;
            // Never log the key or the full prompt; message class + latency only.
            log.warn("LLM call failed (provider=openai, model={}, latencyMs={}): {}",
                    props.getOpenai().getModel(), latency, e.getClass().getSimpleName());
            throw new CeoAnalysisException("OpenAI request failed: " + e.getClass().getSimpleName(), e);
        }
    }

    private String extractContent(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText(null);
            }
            return null;
        } catch (Exception e) {
            throw new CeoAnalysisException("Malformed OpenAI response envelope.", e);
        }
    }

    private static String safeKey(String key) {
        return key == null ? "" : key;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public boolean isRealProvider() {
        return props.getOpenai().getApiKey() != null && !props.getOpenai().getApiKey().isBlank();
    }

    /** Exposed for logging/diagnostics without leaking the key. */
    public Duration requestTimeout() {
        return Duration.ofSeconds(props.getRequestTimeoutSeconds());
    }
}
