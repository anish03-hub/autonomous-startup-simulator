package com.startupsimulator.agent;

import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FinanceAgent}. The LLM seam is always a
 * {@link FakeLLMService} — no real API calls. Phase 2 semantics: in REAL mode the
 * model's financial values are authoritative (runway recomputed, but costs/burn/
 * health are not clamped); an invalid response is an explicit failure with no
 * fabrication. SCRIPTED_DEMO runs the deterministic budget with zero LLM calls.
 */
class FinanceAgentTest {

    private static final FinanceAnalysisResponse VALID = new FinanceAnalysisResponse(
            18_000d,   // developmentCost
            9_000d,    // marketingBudget
            2_000d,    // infrastructureCost
            1_500d,    // operatingCost
            60_000d,   // startingCapital
            4_200d,    // monthlyBurn
            2_500d,    // projectedMonthlyRevenue
            "Break-even around month 15 at ~900 Pro users.",
            70,        // financialHealth
            List.of("Runway is tight before revenue ramps"));

    private StartupContext newContextWithUpstream() {
        Startup s = new Startup();
        s.setId(42L);
        s.setName("FlowCast");
        s.setOriginalIdea("An app that helps freelancers forecast their irregular income.");
        s.setExecutiveSummary("Become the default budgeting copilot for freelancers.");
        StartupContext ctx = new StartupContext(s);
        // Simulate Development + Marketing having run before Finance.
        ctx.getTechnicalPlan().setTimeline("Weeks 1-6 core engine; Weeks 7-12 scanner and polish.");
        ctx.getTechnicalPlan().setEstimatedEngineeringMonths(5.0);
        ctx.getMarketingPlan().setPricingStrategy("Freemium with a $9/mo Pro tier.");
        ctx.getMarketingPlan().setGoToMarket("Launch to a freelancer beachhead.");
        return ctx;
    }

    private LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** REAL mode. */
    private FinanceAgent agent(FakeLLMService llm) {
        return new FinanceAgent(llm, realProps());
    }

    /** SCRIPTED_DEMO mode. */
    private FinanceAgent scriptedAgent(FakeLLMService llm) {
        return new FinanceAgent(llm, new LlmProperties());
    }

    // ---- Required test #6: Finance LLM financial values survive --------------

    @Test
    void successfulStructuredResponse_isAppliedToStartup() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isTrue();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("fake");
        assertThat(llm.structuredCalls).isEqualTo(1);

        // The model's exact financial values survive — not clamped or replaced.
        assertThat(ctx.getBudget().getDevelopmentCost()).isEqualTo(18_000d);
        assertThat(ctx.getBudget().getMonthlyBurn()).isEqualTo(4_200d);
        assertThat(ctx.getBudget().getBreakEvenAssumption()).isEqualTo(VALID.breakEvenAssumption());
        assertThat(ctx.getBudget().isAnalysisFailed()).isFalse();
        assertThat(ctx.getStartup().getFinancialHealth()).isEqualTo(70);
        // Runway is recomputed from capital, upfront cost and burn.
        assertThat(ctx.getBudget().getRunwayMonths()).isGreaterThan(0);
    }

    @Test
    void promptBuildsOnUpstreamPlans() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithUpstream();

        agent(llm).runAnalysis(ctx);

        assertThat(llm.lastUserPrompt)
                .contains("CEO")
                .contains("Become the default budgeting copilot for freelancers.")
                // Upstream Development timeline and Marketing pricing are injected.
                .contains("Weeks 1-6 core engine")
                .contains("Freemium with a $9/mo Pro tier.");
    }

    // ---- Required test #8 (Finance): invalid output handled explicitly -------

    @Test
    void invalidResponse_recordsExplicitFailureWithoutFabrication() {
        // developmentCost 0 and blank break-even → isValid() false.
        FinanceAnalysisResponse invalid = new FinanceAnalysisResponse(
                0, 0, 0, 0, null, 0, 0, null, null, List.of());
        FakeLLMService llm = FakeLLMService.returning(invalid);
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("failed");
        assertThat(llm.structuredCalls).isEqualTo(1);

        assertThat(ctx.getBudget().isAnalysisFailed()).isTrue();
        assertThat(ctx.getBudget().getAnalysisError()).isNotBlank();
        assertThat(ctx.getBudget().getAnalysisProvider()).isEqualTo("failed");
        // No fabrication: no deterministic budget was written over the failed call.
        assertThat(ctx.getBudget().getDevelopmentCost()).isZero();
        assertThat(ctx.getBudget().getBreakEvenAssumption()).isNull();
    }

    // ---- Required test #2 (Finance): SCRIPTED_DEMO makes no real call --------

    @Test
    void scriptedModeDoesNotInvokeTheRealLlm() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        // Deterministic budget — NOT the model's — is used.
        assertThat(ctx.getBudget().getDevelopmentCost())
                .isGreaterThan(0)
                .isNotEqualTo(18_000d);
    }

    @Test
    void scriptedOfflineProvider_usesDeterministicAnalysisWithoutCallingLlm() {
        FakeLLMService llm = FakeLLMService.offline();
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        assertThat(ctx.getBudget().getDevelopmentCost()).isGreaterThan(0);
    }

    @Test
    void agentTypeIsFinance() {
        assertThat(scriptedAgent(FakeLLMService.offline()).type()).isEqualTo(AgentType.FINANCE);
    }
}
