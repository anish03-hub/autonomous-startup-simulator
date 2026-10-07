package com.startupsimulator.agent;

import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DeveloperAgent}. The LLM seam is always a
 * {@link FakeLLMService} — no real API calls. Phase 2 semantics: in REAL mode the
 * model's feature set and technical plan are authoritative (no scripted debate
 * anchors are injected); an invalid response is an explicit failure with no
 * fabrication. SCRIPTED_DEMO runs the deterministic analysis with zero LLM calls.
 */
class DeveloperAgentTest {

    private static final DeveloperAnalysisResponse VALID = new DeveloperAnalysisResponse(
            "Modular monolith: React SPA + Spring Boot API + PostgreSQL.",
            List.of("React", "TypeScript", "Spring Boot", "PostgreSQL"),
            "Weeks 1-6 core engine; Weeks 7-12 scanner and polish.",
            List.of("Model accuracy needs real-world data", "Scope creep from social features"),
            5.0,
            80,
            "An AI copilot that turns bank data into a live cash-flow forecast.",
            List.of(
                    new DeveloperAnalysisResponse.FeatureProposal(
                            "Forecasting engine", "Turns bank data into a runway forecast.", true, 4),
                    new DeveloperAnalysisResponse.FeatureProposal(
                            "Bank import", "Secure bank-account import.", true, 3)));

    private StartupContext newContextWithCeoAnalysis() {
        Startup s = new Startup();
        s.setId(42L);
        s.setName("FlowCast");
        s.setOriginalIdea("An app that helps freelancers forecast their irregular income.");
        // Simulate the CEO having run first — the department must build on this.
        s.setExecutiveSummary("Become the default budgeting copilot for freelancers.");
        s.setProblem("Freelancers lack a simple way to forecast irregular income.");
        s.setTargetAudience("Independent freelancers and solo consultants");
        s.setValueProposition("Know your runway in seconds, not spreadsheets.");
        s.setMvpDirection("Start with the forecasting engine and bank import; defer social features.");
        return new StartupContext(s);
    }

    private LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** REAL mode. */
    private DeveloperAgent agent(FakeLLMService llm) {
        return new DeveloperAgent(llm, realProps());
    }

    /** SCRIPTED_DEMO mode. */
    private DeveloperAgent scriptedAgent(FakeLLMService llm) {
        return new DeveloperAgent(llm, new LlmProperties());
    }

    // ---- Required test #4: Development LLM MVP features survive unchanged ----

    @Test
    void successfulStructuredResponse_isAppliedToStartup() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithCeoAnalysis();

        AgentAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isTrue();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("fake");
        assertThat(llm.structuredCalls).isEqualTo(1);

        assertThat(ctx.getTechnicalPlan().getArchitecture()).isEqualTo(VALID.architecture());
        assertThat(ctx.getTechnicalPlan().getTechStack()).contains("Spring Boot");
        assertThat(ctx.getTechnicalPlan().getEstimatedEngineeringMonths()).isEqualTo(5.0);
        assertThat(ctx.getTechnicalPlan().isAnalysisFailed()).isFalse();
        assertThat(ctx.getStartup().getTechnicalFeasibility()).isEqualTo(80);
        // The model's exact feature set survives...
        assertThat(ctx.getMvpFeatures()).extracting("name")
                .contains("Forecasting engine", "Bank import");
        // ...and the scripted debate anchors are NOT injected over genuine model output.
        assertThat(ctx.getMvpFeatures()).extracting("name")
                .doesNotContain("Product scanner", "Community feed");
    }

    @Test
    void promptBuildsOnCeoAnalysis() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithCeoAnalysis();

        agent(llm).runAnalysis(ctx);

        assertThat(llm.lastSystemPrompt).isNotBlank();
        // The CEO's strategic analysis is injected verbatim so Development builds on it.
        assertThat(llm.lastUserPrompt)
                .contains("CEO")
                .contains("Become the default budgeting copilot for freelancers.")
                .contains("Freelancers lack a simple way to forecast irregular income.")
                .contains("FlowCast");
    }

    // ---- Required test #8 (Development): invalid output handled explicitly ---

    @Test
    void invalidResponse_recordsExplicitFailureWithoutFabrication() {
        // Missing architecture/timeline and empty stack → isValid() false.
        DeveloperAnalysisResponse invalid = new DeveloperAnalysisResponse(
                null, List.of(), null, List.of(), 0, null, null, List.of());
        FakeLLMService llm = FakeLLMService.returning(invalid);
        StartupContext ctx = newContextWithCeoAnalysis();

        AgentAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("failed");
        assertThat(llm.structuredCalls).isEqualTo(1);

        assertThat(ctx.getTechnicalPlan().isAnalysisFailed()).isTrue();
        assertThat(ctx.getTechnicalPlan().getAnalysisError()).isNotBlank();
        assertThat(ctx.getTechnicalPlan().getAnalysisProvider()).isEqualTo("failed");
        // No fabrication: no deterministic plan or anchor features were written.
        assertThat(ctx.getTechnicalPlan().getArchitecture()).isNull();
        assertThat(ctx.getMvpFeatures()).isEmpty();
    }

    // ---- Required test #2 (Development): SCRIPTED_DEMO makes no real call ----

    @Test
    void scriptedModeDoesNotInvokeTheRealLlm() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithCeoAnalysis();

        AgentAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        // Deterministic path guarantees the scripted debate anchors.
        assertThat(ctx.getMvpFeatures()).extracting("name")
                .contains("Product scanner", "Community feed");
    }

    @Test
    void scriptedOfflineProvider_usesDeterministicAnalysisWithoutCallingLlm() {
        FakeLLMService llm = FakeLLMService.offline();
        StartupContext ctx = newContextWithCeoAnalysis();

        AgentAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        assertThat(ctx.getTechnicalPlan().getArchitecture()).isNotBlank();
    }

    @Test
    void agentTypeIsDevelopment() {
        assertThat(scriptedAgent(FakeLLMService.offline()).type()).isEqualTo(AgentType.DEVELOPMENT);
    }
}
