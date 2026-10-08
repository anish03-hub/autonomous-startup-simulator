package com.startupsimulator.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Startup;
import com.startupsimulator.resilience.RetryExecutor;
import com.startupsimulator.resilience.Sleeper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiLLMServiceTest {

    private ObjectMapper objectMapper;
    private LlmProperties props;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        props = new LlmProperties();
        props.setProvider("gemini");
        props.setMode(AiExecutionMode.REAL);
        props.getGemini().setApiKey("test-gemini-key-xyz");
        props.getGemini().setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        props.getGemini().setModel("gemini-3.8-flash");
    }

    @Test
    void providerSelection_returnsGemini() {
        assertThat(props.getProvider()).isEqualTo("gemini");
        GeminiLLMService service = new GeminiLLMService(props, objectMapper);
        assertThat(service.provider()).isEqualTo("gemini");
        assertThat(service.isRealProvider()).isTrue();
    }

    @Test
    void configuration_hasCorrectBaseUrlAndModelDefaults() {
        LlmProperties defaultProps = new LlmProperties();
        assertThat(defaultProps.getGemini().getBaseUrl()).isEqualTo("https://generativelanguage.googleapis.com/v1beta");
        assertThat(defaultProps.getGemini().getModel()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    void generate_sendsCorrectXGoogApiKeyHeaderAndUrl() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String expectedUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini-key-xyz"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("User prompt"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Gemini says hello\"}]}}]}", MediaType.APPLICATION_JSON));

        GeminiLLMService service = new GeminiLLMService(props, objectMapper, restClient, new RetryExecutor(Sleeper.NO_OP));
        String response = service.generate("System prompt", "User prompt");

        assertThat(response).isEqualTo("Gemini says hello");
        mockServer.verify();
    }

    @Test
    void apiKey_isNeverLoggedOrExposedInExceptions() {
        props.getGemini().setApiKey("super-secret-gemini-key-12345");
        GeminiLLMService service = new GeminiLLMService(props, objectMapper);
        assertThat(service.isRealProvider()).isTrue();

        props.getGemini().setBaseUrl("http://localhost:65534");
        GeminiLLMService failingService = new GeminiLLMService(props, objectMapper);

        assertThatThrownBy(() -> failingService.generate("system", "user"))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageNotContaining("super-secret-gemini-key-12345");
    }

    @Test
    void successfulResponseParsing_structuredOutput() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String jsonResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"decision\\\":\\\"PROCEED\\\",\\\"confidence\\\":0.95}\"}]}}]}";
        String expectedUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GeminiLLMService service = new GeminiLLMService(props, objectMapper, restClient, new RetryExecutor(Sleeper.NO_OP));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = service.generateStructured("System prompt", "User prompt", Map.class);

        assertThat(result.get("decision")).isEqualTo("PROCEED");
        assertThat(result.get("confidence")).isEqualTo(0.95);
        mockServer.verify();
    }

    @Test
    void malformedResponseHandling_throwsCeoAnalysisException() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String expectedUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess("{\"unexpected\": true}", MediaType.APPLICATION_JSON));

        GeminiLLMService service = new GeminiLLMService(props, objectMapper, restClient, new RetryExecutor(Sleeper.NO_OP));

        assertThatThrownBy(() -> service.generate("System prompt", "User prompt"))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("Gemini returned an empty completion");
    }

    @Test
    void http429And500Retry_succeedsOnThirdAttempt() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String expectedUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

        // Attempt 1: 429 Too Many Requests
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // Attempt 2: 500 Internal Server Error
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Attempt 3: 200 OK
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Success after rate limit\"}]}}]}", MediaType.APPLICATION_JSON));

        RetryExecutor retryExecutor = new RetryExecutor(Sleeper.NO_OP);
        GeminiLLMService service = new GeminiLLMService(props, objectMapper, restClient, retryExecutor);

        String result = service.generate("System prompt", "User prompt");
        assertThat(result).isEqualTo("Success after rate limit");
        mockServer.verify();
    }

    @Test
    void retryExhaustion_throwsExplicitException() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String expectedUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":{\"code\":503,\"message\":\"The model is overloaded.\",\"status\":\"UNAVAILABLE\"}}"));
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":{\"code\":503,\"message\":\"The model is overloaded.\",\"status\":\"UNAVAILABLE\"}}"));
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":{\"code\":503,\"message\":\"The model is overloaded.\",\"status\":\"UNAVAILABLE\"}}"));

        RetryExecutor retryExecutor = new RetryExecutor(Sleeper.NO_OP);
        GeminiLLMService service = new GeminiLLMService(props, objectMapper, restClient, retryExecutor);

        assertThatThrownBy(() -> service.generate("System prompt", "User prompt"))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("HTTP 503")
                .hasMessageContaining("The model is overloaded");
        mockServer.verify();
    }

    @Test
    void http503Response_capturesResponseBodyAndIsRetryable() {
        org.springframework.web.client.HttpServerErrorException ex503 =
                org.springframework.web.client.HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"error\":{\"code\":503,\"message\":\"The model is overloaded.\"}}".getBytes(), null);
        assertThat(com.startupsimulator.resilience.FailureClassifier.isRetryable(ex503)).isTrue();
    }

    @Test
    void authenticationFailure_throwsExplicitExceptionWithoutRetrying() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String expectedUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

        // 401 Unauthorized (non-transient)
        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        RetryExecutor retryExecutor = new RetryExecutor(Sleeper.NO_OP);
        GeminiLLMService service = new GeminiLLMService(props, objectMapper, restClient, retryExecutor);

        assertThatThrownBy(() -> service.generate("System prompt", "User prompt"))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("Gemini request failed");
        mockServer.verify();
    }

    @Test
    void realMode_hasNoDeterministicFallbackWhenProviderFails() {
        props.setMode(AiExecutionMode.REAL);
        props.getGemini().setApiKey(""); // Missing key

        GeminiLLMService service = new GeminiLLMService(props, objectMapper);
        CeoAgent agent = new CeoAgent(service, props);

        Startup startup = new Startup();
        startup.setId(1L);
        startup.setName("Test Startup");
        startup.setOriginalIdea("Test Idea");
        StartupContext ctx = new StartupContext(startup);

        CeoAnalysisOutcome outcome = agent.runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.provider()).isEqualTo("unavailable");
        assertThat(ctx.getStartup().isCeoAnalysisFailed()).isTrue();
        assertThat(ctx.getStartup().getExecutiveSummary()).isNull();
    }

    @Test
    void missingApiKey_producesExplicitProviderFailure() {
        props.getGemini().setApiKey(""); // Empty key
        GeminiLLMService service = new GeminiLLMService(props, objectMapper);

        assertThat(service.isRealProvider()).isFalse();
        assertThatThrownBy(() -> service.generate("System prompt", "User prompt"))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("Gemini API key is not configured (set GEMINI_API_KEY)");
    }
}
