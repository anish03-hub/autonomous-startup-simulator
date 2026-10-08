package com.startupsimulator.config;

import com.startupsimulator.agent.CeoAgent;
import com.startupsimulator.agent.CeoAnalysisException;
import com.startupsimulator.agent.CeoAnalysisOutcome;
import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.agent.MockLLMService;
import com.startupsimulator.agent.OpenAiLLMService;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.model.Startup;
import com.startupsimulator.resilience.RetryExecutor;
import com.startupsimulator.resilience.RetryPolicy;
import com.startupsimulator.resilience.RetryableLLMService;
import com.startupsimulator.resilience.Sleeper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionLlmEnforcementTest {

    @Test
    void productionConfigurationDefaultsToJustDoWorkProvider() {
        LlmProperties props = new LlmProperties();
        assertThat(props.getProvider()).isEqualTo("justdowork");
    }

    @Test
    void fakeLlmServiceIsUsedOnlyByTests() {
        // Prove FakeLLMService is not annotated as a Spring production bean (@Service or @Component)
        assertThat(FakeLLMService.class.isAnnotationPresent(Service.class)).isFalse();
        assertThat(FakeLLMService.class.isAnnotationPresent(Component.class)).isFalse();
    }

    @Test
    void missingJustDoWorkCredentials_producesExplicitFailure() {
        LlmProperties props = new LlmProperties();
        props.setMode(AiExecutionMode.REAL);
        props.getJustdowork().setApiKey(""); // Missing key
        com.startupsimulator.agent.JustDoWorkLLMService service = new com.startupsimulator.agent.JustDoWorkLLMService(props, new ObjectMapper());

        assertThatThrownBy(() -> service.generateStructured("system", "user", Object.class))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("JustDoWork API key is not configured");
    }

    @Test
    void missingGeminiCredentials_producesExplicitFailure() {
        LlmProperties props = new LlmProperties();
        props.setMode(AiExecutionMode.REAL);
        props.getGemini().setApiKey(""); // Missing key
        com.startupsimulator.agent.GeminiLLMService geminiService = new com.startupsimulator.agent.GeminiLLMService(props, new ObjectMapper());

        assertThatThrownBy(() -> geminiService.generateStructured("system", "user", Object.class))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("Gemini API key is not configured");
    }

    @Test
    void missingOpenAiCredentials_producesExplicitFailure() {
        LlmProperties props = new LlmProperties();
        props.setMode(AiExecutionMode.REAL);
        props.getOpenai().setApiKey(""); // Missing key
        OpenAiLLMService openAiService = new OpenAiLLMService(props, new ObjectMapper());

        assertThatThrownBy(() -> openAiService.generateStructured("system", "user", Object.class))
                .isInstanceOf(CeoAnalysisException.class)
                .hasMessageContaining("OpenAI API key is not configured");
    }

    @Test
    void providerFailures_remainFailuresAfterRetryExhaustion() {
        CeoAnalysisException transientError = new CeoAnalysisException("Simulated transient timeout", new ResourceAccessException("SocketTimeoutException"));
        FakeLLMService fakeLlm = FakeLLMService.returningSequence(transientError, transientError, transientError);

        RetryPolicy policy = new RetryPolicy(3, 10L, 2.0, 100L);
        RetryExecutor executor = new RetryExecutor(Sleeper.NO_OP);
        RetryableLLMService retryableLlm = new RetryableLLMService(fakeLlm, executor, policy);

        LlmProperties props = new LlmProperties();
        props.setMode(AiExecutionMode.REAL);
        CeoAgent agent = new CeoAgent(retryableLlm, props);

        StartupContext ctx = newContext();
        CeoAnalysisOutcome outcome = agent.runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.provider()).isEqualTo("failed");
        assertThat(ctx.getStartup().isCeoAnalysisFailed()).isTrue();
        assertThat(ctx.getStartup().getExecutiveSummary()).isNull(); // No deterministic fallback
    }

    @Test
    void noDeterministicFallbackIsInvokedInRealModeWhenProviderFails() {
        MockLLMService mockLlm = new MockLLMService(); // Not a real provider
        LlmProperties props = new LlmProperties();
        props.setMode(AiExecutionMode.REAL);
        CeoAgent agent = new CeoAgent(mockLlm, props);

        StartupContext ctx = newContext();
        CeoAnalysisOutcome outcome = agent.runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.provider()).isEqualTo("unavailable");
        assertThat(ctx.getStartup().isCeoAnalysisFailed()).isTrue();
        assertThat(ctx.getStartup().getExecutiveSummary()).isNull();
    }

    private StartupContext newContext() {
        Startup startup = new Startup();
        startup.setId(1L);
        startup.setName("Test Startup");
        startup.setOriginalIdea("Test Idea");
        return new StartupContext(startup);
    }
}
