package com.startupsimulator.agent;

import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MarketingAgent}. The LLM seam is always a
 * {@link FakeLLMService} — no real API calls. Phase 2 semantics: in REAL mode the
 * model's positioning/pricing/GTM are authoritative (no readiness floor, no
 * scanner tail injected); an invalid response is an explicit failure with no
 * fabrication. SCRIPTED_DEMO runs the deterministic analysis with zero LLM calls.
 */
class MarketingAgentTest {

    private static final MarketingAnalysisResponse VALID = new MarketingAnalysisResponse(
            "The fastest runway forecast for freelancers.",
            "Incumbents are generic spreadsheets; niche apps lack personalisation.",
            "Freemium with a $9/mo Pro tier.",
            List.of("Content/SEO", "Short-form video", "Referral loop"),
            "Launch to a freelancer beachhead, drive word-of-mouth, then expand.",
            "freelancers and solo consultants",
            "Know your runway in seconds.",
            "Freemium SaaS with a Pro subscription.",
            List.of("Freemium conversion must clear ~3-5%"));

    private StartupContext newContextWithUpstream() {
        Startup s = new Startup();
        s.setId(42L);
        s.setName("FlowCast");
        s.setOriginalIdea("An app that helps freelancers forecast their irregular income.");
        s.setExecutiveSummary("Become the default budgeting copilot for freelancers.");
        s.setValueProposition("Know your runway in seconds, not spreadsheets.");
        s.setSolution("An AI copilot that turns bank data into a live cash-flow forecast.");
        StartupContext ctx = new StartupContext(s);
        // Simulate Development having run before Marketing.
        ctx.getTechnicalPlan().setArchitecture("Modular monolith: React + Spring Boot + PostgreSQL.");
        ctx.getTechnicalPlan().setTimeline("Weeks 1-6 core engine; Weeks 7-12 scanner and polish.");
        return ctx;
    }

    private LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** REAL mode. */
    private MarketingAgent agent(FakeLLMService llm) {
        return new MarketingAgent(llm, realProps());
    }

    /** SCRIPTED_DEMO mode. */
    private MarketingAgent scriptedAgent(FakeLLMService llm) {
        return new MarketingAgent(llm, new LlmProperties());
    }

    // ---- Required test #5: Marketing LLM pricing survives --------------------

    @Test
    void successfulStructuredResponse_isAppliedToStartup() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isTrue();
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.provider()).isEqualTo("fake");
        assertThat(llm.structuredCalls).isEqualTo(1);

        assertThat(ctx.getMarketingPlan().getPositioning()).isEqualTo(VALID.positioning());
        assertThat(ctx.getMarketingPlan().getPricingStrategy()).isEqualTo(VALID.pricingStrategy());
        assertThat(ctx.getMarketingPlan().getChannels()).contains("Content/SEO");
        assertThat(ctx.getMarketingPlan().getGoToMarket()).isEqualTo(VALID.goToMarket());
        assertThat(ctx.getMarketingPlan().isAnalysisFailed()).isFalse();
        assertThat(ctx.getStartup().getBusinessModel()).isEqualTo(VALID.businessModel());
    }

    @Test
    void promptBuildsOnCeoAndDevelopmentScope() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithUpstream();

        agent(llm).runAnalysis(ctx);

        assertThat(llm.lastUserPrompt)
                .contains("CEO")
                .contains("Become the default budgeting copilot for freelancers.")
                // The Development architecture and timeline are injected as upstream scope.
                .contains("Modular monolith: React + Spring Boot + PostgreSQL.")
                .contains("Weeks 1-6 core engine");
    }

    // ---- Required test #8 (Marketing): invalid output handled explicitly -----

    @Test
    void invalidResponse_recordsExplicitFailureWithoutFabrication() {
        MarketingAnalysisResponse invalid = new MarketingAnalysisResponse(
                null, null, null, List.of(), null, null, null, null, List.of());
        FakeLLMService llm = FakeLLMService.returning(invalid);
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = agent(llm).runAnalysis(ctx);

        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("failed");
        assertThat(llm.structuredCalls).isEqualTo(1);

        assertThat(ctx.getMarketingPlan().isAnalysisFailed()).isTrue();
        assertThat(ctx.getMarketingPlan().getAnalysisError()).isNotBlank();
        assertThat(ctx.getMarketingPlan().getAnalysisProvider()).isEqualTo("failed");
        // No fabrication: no deterministic positioning was written.
        assertThat(ctx.getMarketingPlan().getPositioning()).isNull();
    }

    // ---- Required test #2 (Marketing): SCRIPTED_DEMO makes no real call ------

    @Test
    void scriptedModeDoesNotInvokeTheRealLlm() {
        FakeLLMService llm = FakeLLMService.returning(VALID);
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        // Deterministic positioning — NOT the model's — is used.
        assertThat(ctx.getMarketingPlan().getPositioning())
                .isNotBlank()
                .isNotEqualTo(VALID.positioning());
    }

    @Test
    void scriptedOfflineProvider_usesDeterministicAnalysisWithoutCallingLlm() {
        FakeLLMService llm = FakeLLMService.offline();
        StartupContext ctx = newContextWithUpstream();

        AgentAnalysisOutcome outcome = scriptedAgent(llm).runAnalysis(ctx);

        assertThat(outcome.usedRealLlm()).isFalse();
        assertThat(outcome.provider()).isEqualTo("mock");
        assertThat(llm.structuredCalls).isZero();
        assertThat(ctx.getMarketingPlan().getPositioning()).isNotBlank();
    }

    @Test
    void agentTypeIsMarketing() {
        assertThat(scriptedAgent(FakeLLMService.offline()).type()).isEqualTo(AgentType.MARKETING);
    }
}
