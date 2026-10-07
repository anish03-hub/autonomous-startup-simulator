package com.startupsimulator.agent;

import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CeoAgent}. The LLM seam is always a {@link FakeLLMService}
 * — no real API calls are made. Phase 2 semantics: the execution <em>mode</em>
 * (REAL vs SCRIPTED_DEMO), not merely provider availability, decides the path.
 *
 * <p>In REAL mode a valid structured response is written as the authoritative
 * startup state; an invalid/failed response is an <em>explicit failure</em> with no
 * fabricated fallback. In SCRIPTED_DEMO mode the deterministic analysis runs with
 * zero LLM calls (demo/test safe, no network).
 */
class CeoAgentTest {

    private static final CeoAnalysisResponse VALID = new CeoAnalysisResponse(
            "Become the default budgeting copilot for freelancers.",
            "Freelancers lack a simple way to forecast irregular income.",
            "Independent freelancers and solo consultants",
            "An AI copilot that turns bank data into a live cash-flow forecast.",
            "Know your runway in seconds, not spreadsheets.",
            "Freemium SaaS with a Pro subscription.",
            List.of("Ship a focused MVP", "Protect runway"),
            List.of("Freelancers feel this pain acutely"),
            List.of("Underserved niche"),
            List.of("Adoption risk if value is not obvious", "Scope creep"),
            "Start with the forecasting engine and bank import; defer social features."
    );

    private StartupContext newContext() {
        Startup s = new Startup();
        s.setId(42L);
        s.setName("FlowCast");
        s.setOriginalIdea("An app that helps freelancers forecast their irregular income.");
        return new StartupContext(s);
    }

    private LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** REAL mode: genuine LLM reasoning runs when a real provider is present. */
    private CeoAgent agent(FakeLLMService llm) {
        return new CeoAgent(llm, realProps());
    }

    /** SCRIPTED_DEMO mode: deterministic, never calls a real LLM. */
    private CeoAgent scriptedAgent(FakeLLMService llm) {
        return new CeoAgent(llm, new LlmProperties());
    }

    // ---- Required test #1: REAL mode invokes the structured LLM call ---------

    @Test
    void realModeInvokesGenerateStructured() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        agent(llm).runAnalysis(newContext());
        assertThat(llm.structuredCalls).isEqualTo(1);
    }

    // ---- Required test #3: CEO LLM output survives into persisted state ------

    @Test
    void successfulStructuredResponse_isAppliedToStartup() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContext();

        CeoAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isTrue();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("fake");
        assertThat(llm.structuredCalls).isEqualTo(1);

        Startup s = ctx.getStartup();
        assertThat(s.getExecutiveSummary()).isEqualTo(VALID.executiveSummary());
        assertThat(s.getProblem()).isEqualTo(VALID.problem());
        assertThat(s.getMvpDirection()).isEqualTo(VALID.recommendedMvpDirection());
        assertThat(s.getStrategicObjectives()).contains("Ship a focused MVP");
        assertThat(s.getAssumptions()).contains("Freelancers feel this pain acutely");
        assertThat(s.isCeoAnalysisFailed()).isFalse();
        assertThat(s.getCeoAnalysisError()).isNull();
        assertThat(s.getCeoAnalysisProvider()).isEqualTo("fake");
        // Blank narrative fields get filled from the CEO's analysis.
        assertThat(s.getTargetAudience()).isEqualTo(VALID.targetCustomer());
        assertThat(s.getValueProposition()).isEqualTo(VALID.valueProposition());
        // Risks flow into the shared context.
        assertThat(ctx.getRisks()).contains("Scope creep");
    }

    // ---- Required test #7: no post-processing silently replaces LLM values ---

    @Test
    void realModeDoesNotOverwriteLlmValuesWithDeterministicDefaults() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContext();

        agent(llm).runAnalysis(ctx);

        Startup s = ctx.getStartup();
        // The authoritative fields are EXACTLY the model's values — not clamped,
        // not replaced by the deterministic heuristic text.
        assertThat(s.getExecutiveSummary()).isEqualTo(VALID.executiveSummary());
        assertThat(s.getProblem()).isEqualTo(VALID.problem());
        assertThat(s.getMvpDirection()).isEqualTo(VALID.recommendedMvpDirection());
        assertThat(s.getBusinessModel()).isEqualTo(VALID.businessModel());
        // The deterministic marketReadiness floor (55) is NOT applied on the real path.
        assertThat(s.getMarketReadiness()).isZero();
    }

    // ---- Required test #8: invalid LLM output handled explicitly -------------

    @Test
    void invalidResponse_recordsExplicitFailureWithoutFabrication() {
        // Missing required fields → isValid() is false → validation failure.
        CeoAnalysisResponse invalid = new CeoAnalysisResponse(
                "summary only", null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), null);
        FakeLLMService llm = FakeLLMService.returning(invalid);
        StartupContext ctx = newContext();

        CeoAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("failed");
        assertThat(llm.structuredCalls).isEqualTo(1);

        Startup s = ctx.getStartup();
        assertThat(s.isCeoAnalysisFailed()).isTrue();
        assertThat(s.getCeoAnalysisError()).isNotBlank();
        assertThat(s.getCeoAnalysisProvider()).isEqualTo("failed");
        // No fabrication: nothing deterministic was written over the failed call.
        assertThat(s.getExecutiveSummary()).isNull();
        assertThat(s.getProblem()).isNull();
    }

    @Test
    void llmFailure_recordsExplicitFailureWithoutFabrication() {
        FakeLLMService llm = FakeLLMService.failing();
        StartupContext ctx = newContext();

        CeoAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.error()).isNotBlank();
        assertThat(outcome.provider()).isEqualTo("failed");

        Startup s = ctx.getStartup();
        assertThat(s.isCeoAnalysisFailed()).isTrue();
        assertThat(s.getCeoAnalysisProvider()).isEqualTo("failed");
        // No fabricated narrative on the real path.
        assertThat(s.getExecutiveSummary()).isNull();
    }

    @Test
    void realModeWithoutProvider_recordsUnavailableFailure() {
        // REAL requested but the provider cannot reach a model → explicit failure.
        FakeLLMService llm = FakeLLMService.offline();
        StartupContext ctx = newContext();

        CeoAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("unavailable");
        assertThat(llm.structuredCalls).isZero();
        assertThat(ctx.getStartup().isCeoAnalysisFailed()).isTrue();
        assertThat(ctx.getStartup().getExecutiveSummary()).isNull();
    }

    // ---- Required test #2: SCRIPTED_DEMO does not invoke a real LLM ----------

    @Test
    void scriptedModeDoesNotInvokeTheRealLlm() {
        // Real provider present, but SCRIPTED_DEMO mode → deterministic, zero calls.
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContext();

        CeoAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        // The deterministic analysis — NOT the model's response — is used.
        assertThat(ctx.getStartup().getExecutiveSummary())
                .isNotBlank()
                .isNotEqualTo(VALID.executiveSummary());
    }

    @Test
    void scriptedOfflineProvider_usesDeterministicAnalysisWithoutCallingLlm() {
        FakeLLMService llm = FakeLLMService.offline();
        StartupContext ctx = newContext();

        CeoAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        assertThat(ctx.getStartup().isCeoAnalysisFailed()).isFalse();
        assertThat(ctx.getStartup().getExecutiveSummary()).isNotBlank();
    }

    @Test
    void contextIsPassedToTheModel() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContext();

        agent(llm).runAnalysis(ctx);

        assertThat(llm.lastSystemPrompt).isNotBlank();
        assertThat(llm.lastUserPrompt)
                .contains("FlowCast")
                .contains("freelancers");
    }

    @Test
    void analysisBuildsRoadmapAndIsIdempotentAcrossRetries() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContext();
        CeoAgent ceo = agent(llm);

        ceo.runAnalysis(ctx);
        int afterFirst = ctx.getRoadmap().size();
        ceo.runAnalysis(ctx); // retry
        int afterRetry = ctx.getRoadmap().size();

        assertThat(afterFirst).isGreaterThan(0);
        assertThat(afterRetry).isEqualTo(afterFirst); // no duplicate milestones
    }

    @Test
    void agentTypeIsCeo() {
        assertThat(scriptedAgent(FakeLLMService.offline()).type()).isEqualTo(AgentType.CEO);
    }
}
