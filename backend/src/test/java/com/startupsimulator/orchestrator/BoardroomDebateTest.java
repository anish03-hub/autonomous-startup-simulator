package com.startupsimulator.orchestrator;

import com.startupsimulator.agent.CeoAgent;
import com.startupsimulator.agent.DebateResponse;
import com.startupsimulator.agent.DecisionSynthesisResponse;
import com.startupsimulator.agent.DeveloperAgent;
import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.agent.FinanceAgent;
import com.startupsimulator.agent.LLMService;
import com.startupsimulator.agent.MarketingAgent;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Debate;
import com.startupsimulator.model.Decision;
import com.startupsimulator.model.MvpFeature;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.DebateStatus;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.service.AgentService;
import com.startupsimulator.service.DebateService;
import com.startupsimulator.service.DecisionService;
import com.startupsimulator.service.EventService;
import com.startupsimulator.service.MessageService;
import com.startupsimulator.service.StartupContextService;
import com.startupsimulator.service.StartupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Phase 2C {@link BoardroomDebate}. The LLM seam is always a
 * {@link FakeLLMService} — NO real API calls. Services are Mockito mocks; the four
 * agents are real instances backed by an offline fake so their deterministic
 * fallbacks work. These tests prove the debate is a genuine multi-agent exchange:
 * every Round-2/Round-3/synthesis prompt actually contains the earlier rounds'
 * messages (§21), the CEO synthesis drives the final decision, features are
 * deferred from the decision (§11), and a failed turn never crashes the run (§17).
 */
class BoardroomDebateTest {

    private static final Long STARTUP_ID = 42L;
    private static final Long DEBATE_ID = 7L;
    private static final Long DECISION_ID = 99L;

    private MessageService messageService;
    private DebateService debateService;
    private DecisionService decisionService;
    private EventService eventService;
    private AgentService agentService;
    private StartupContextService contextService;
    private StartupService startupService;
    private CeoAgent ceoAgent;
    private DeveloperAgent developerAgent;
    private MarketingAgent marketingAgent;
    private FinanceAgent financeAgent;

    @BeforeEach
    void setUp() {
        messageService = mock(MessageService.class);
        debateService = mock(DebateService.class);
        decisionService = mock(DecisionService.class);
        eventService = mock(EventService.class);
        agentService = mock(AgentService.class);
        contextService = mock(StartupContextService.class);
        startupService = mock(StartupService.class);

        FakeLLMService offline = FakeLLMService.offline();
        ceoAgent = new CeoAgent(offline, new LlmProperties());
        developerAgent = new DeveloperAgent(offline, new LlmProperties());
        marketingAgent = new MarketingAgent(offline, new LlmProperties());
        financeAgent = new FinanceAgent(offline, new LlmProperties());

        Debate debate = new Debate(STARTUP_ID, "topic", "question");
        debate.setId(DEBATE_ID);
        when(debateService.create(anyLong(), anyString(), anyString())).thenReturn(debate);

        Decision decision = new Decision(STARTUP_ID, DEBATE_ID, "decision", "reason");
        decision.setId(DECISION_ID);
        when(decisionService.createFromSynthesis(anyLong(), anyLong(), any(), any()))
                .thenReturn(decision);
    }

    private LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** REAL mode: the debate runs genuine LLM turns when the provider is real. */
    private BoardroomDebate debate(LLMService llm) {
        return debate(llm, realProps());
    }

    /** SCRIPTED_DEMO mode: the debate runs its deterministic script with zero LLM calls. */
    private BoardroomDebate scriptedDebate(LLMService llm) {
        return debate(llm, new LlmProperties());
    }

    private BoardroomDebate debate(LLMService llm, LlmProperties llmProperties) {
        BoardroomDebate bd = new BoardroomDebate(messageService, debateService, decisionService,
                eventService, agentService, contextService, startupService,
                ceoAgent, developerAgent, marketingAgent, financeAgent, llm, llmProperties);
        ReflectionTestUtils.setField(bd, "tickIntervalMs", 0L);
        return bd;
    }

    /** A context seeded with the four Phase 2B analyses and the debate-anchor features. */
    private StartupContext newContext() {
        Startup s = new Startup();
        s.setId(STARTUP_ID);
        s.setName("GlowRoutine");
        s.setOriginalIdea("An AI-powered app that scans skincare products and recommends a personalized routine.");
        s.setExecutiveSummary("Become the trusted skincare copilot.");
        s.setMvpDirection("Ship a focused scanner-first MVP.");
        s.setProblem("Shoppers cannot tell which products suit their skin.");
        s.setSolution("Scan a product and get a personalized routine.");
        StartupContext ctx = new StartupContext(s);
        ctx.getTechnicalPlan().setArchitecture("Mobile app + vision API + recommendation service.");
        ctx.getTechnicalPlan().setTimeline("~5 months at full scope.");
        ctx.getTechnicalPlan().setEstimatedEngineeringMonths(5);
        ctx.getMarketingPlan().setPositioning("The scanner that personalizes your shelf.");
        ctx.getMarketingPlan().setPricingStrategy("Freemium with a $9/mo Pro tier.");
        ctx.getMarketingPlan().setGoToMarket("Creator-led launch.");
        ctx.getBudget().setStartingCapital(60_000d);
        ctx.getBudget().setMonthlyBurn(4_550d);
        ctx.getBudget().setRunwayMonths(5.8);
        ctx.getBudget().setBreakEvenAssumption("Break-even around month 14.");
        ctx.addFeature(new MvpFeature(STARTUP_ID, "Product scanner",
                "Scan a product and read its ingredients.", true, 3));
        ctx.addFeature(new MvpFeature(STARTUP_ID, "Personalized routine engine",
                "Build a routine from scans.", true, 3));
        ctx.addFeature(new MvpFeature(STARTUP_ID, "Community feed",
                "Social feed of routines.", true, 5));
        return ctx;
    }

    private DebateResponse dr(String marker) {
        return new DebateResponse(marker + " position", marker + " reasoning",
                List.of(marker + " challenge"), List.of(marker + " concession"),
                marker + " recommendation", 70, List.of(marker + " concern"));
    }

    private DecisionSynthesisResponse synthesis(String marker, List<String> deferred) {
        return new DecisionSynthesisResponse(marker + " decision", marker + " rationale",
                List.of("accepted arg"), List.of("rejected arg"),
                "V1 keeps the scanner", deferred,
                List.of("adoption risk"), List.of("ship the scanner"));
    }

    /** A full 10-call sequence: 9 debate turns (Dev/Mkt/Fin × 3 rounds) + CEO synthesis. */
    private FakeLLMService fullSequence(DecisionSynthesisResponse synth) {
        return FakeLLMService.returningSequence(
                dr("DEV_R1"), dr("MKT_R1"), dr("FIN_R1"),
                dr("DEV_R2"), dr("MKT_R2"), dr("FIN_R2"),
                dr("DEV_R3"), dr("MKT_R3"), dr("FIN_R3"),
                synth);
    }

    // ---- shared capture helpers ---------------------------------------------

    /** Every persisted message content, in call order (framing, 9 turns, finance, synthesis). */
    private List<String> messageContents() {
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(messageService, atLeast(1)).create(eq(STARTUP_ID), any(AgentType.class),
                content.capture(), eq(DEBATE_ID), any(), anyString(), any());
        return content.getAllValues();
    }

    /** Every EventType recorded, in call order. */
    private List<EventType> recordedEvents() {
        ArgumentCaptor<EventType> ev = ArgumentCaptor.forClass(EventType.class);
        verify(eventService, atLeast(1)).record(eq(STARTUP_ID), ev.capture(), anyString(), any());
        return ev.getAllValues();
    }

    private MvpFeature feature(StartupContext ctx, String name) {
        return ctx.getMvpFeatures().stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst().orElseThrow();
    }

    // ---- §20: round generation & call budget --------------------------------

    @Test
    void conductMakesExactlyTenStructuredCalls_nineTurnsPlusSynthesis() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        // 3 rounds × 3 departments + 1 CEO synthesis. The CEO R1 framing is
        // deterministic and must NOT hit the model (§5).
        assertThat(llm.structuredCalls).isEqualTo(10);
    }

    @Test
    void round1GeneratesADepartmentPositionForEachAgent() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        List<String> contents = messageContents();
        assertThat(contents).anyMatch(c -> c.contains("DEV_R1"));
        assertThat(contents).anyMatch(c -> c.contains("MKT_R1"));
        assertThat(contents).anyMatch(c -> c.contains("FIN_R1"));
    }

    // ---- §21: every later turn actually sees the earlier rounds -------------

    @Test
    void developerRound2PromptContainsMarketingRound1Message() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        String devR2 = llm.userPrompts.get(3);
        assertThat(devR2).contains("MKT_R1");            // sees Marketing's R1 position
        assertThat(devR2).contains("Marketing");         // and is told to challenge Marketing
        assertThat(devR2.toLowerCase()).contains("challenge");
    }

    @Test
    void marketingRound2PromptContainsDeveloperRound1Message() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        String mktR2 = llm.userPrompts.get(4);
        assertThat(mktR2).contains("DEV_R1");
        assertThat(mktR2).contains("Development");
    }

    @Test
    void financeRound2PromptContainsBothDeveloperAndMarketingRound1() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        String finR2 = llm.userPrompts.get(5);
        assertThat(finR2).contains("DEV_R1");
        assertThat(finR2).contains("MKT_R1");
        assertThat(finR2).contains("Development");        // Finance challenges Development
    }

    @Test
    void round3PromptsContainThePriorRoundsMessages() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        String devR3 = llm.userPrompts.get(6);
        assertThat(devR3).contains("DEV_R1").contains("MKT_R1").contains("FIN_R1");
        assertThat(devR3).contains("DEV_R2").contains("MKT_R2").contains("FIN_R2");
    }

    @Test
    void ceoSynthesisPromptContainsTheEntireDebate() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        String synthesisPrompt = llm.userPrompts.get(9);
        assertThat(synthesisPrompt)
                .contains("DEV_R1").contains("MKT_R1").contains("FIN_R1")
                .contains("DEV_R2").contains("MKT_R2").contains("FIN_R2")
                .contains("DEV_R3").contains("MKT_R3").contains("FIN_R3");
    }

    @Test
    void everyTurnPromptEmbedsThePhase2bAnalyses() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        // The debate is grounded in the REAL Phase 2B outputs, not just the idea.
        for (String prompt : llm.userPrompts) {
            assertThat(prompt)
                    .contains("Mobile app + vision API")       // technical plan architecture
                    .contains("scanner that personalizes")     // marketing positioning
                    .contains("Community feed");                // proposed MVP feature
        }
    }

    // ---- §12: the debate lifecycle events are emitted -----------------------

    @Test
    void debateEmitsTheFullLifecycleOfEvents() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        assertThat(recordedEvents()).contains(
                EventType.DEBATE_STARTED,
                EventType.DEBATE_ROUND_STARTED,
                EventType.AGENT_DEBATE_STARTED,
                EventType.AGENT_DEBATE_MESSAGE,
                EventType.AGENT_DEBATE_COMPLETED,
                EventType.DEBATE_ROUND_COMPLETED,
                EventType.DEBATE_COMPLETED,
                EventType.CEO_SYNTHESIS_STARTED,
                EventType.CEO_SYNTHESIS_COMPLETED,
                EventType.DECISION_STARTED,
                EventType.DECISION_COMPLETED);
    }

    @Test
    void everyPersistedDebateMessageCarriesRoundAndType() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        // framing + 9 turns + finance recompute + CEO synthesis = 12 debate messages.
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> round = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> messageType = ArgumentCaptor.forClass(String.class);
        verify(messageService, times(12)).create(eq(STARTUP_ID), any(AgentType.class),
                content.capture(), eq(DEBATE_ID), round.capture(), messageType.capture(), any());

        assertThat(messageType.getAllValues()).contains("FRAMING", "POSITION", "CHALLENGE",
                "CONVERGENCE", "SYNTHESIS");
        assertThat(round.getAllValues()).allMatch(r -> r >= 1 && r <= 3);
    }

    // ---- §9/§10: the final decision comes from the CEO synthesis ------------

    @Test
    void finalDecisionIsBuiltFromTheCeoSynthesis() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        ArgumentCaptor<DecisionSynthesisResponse> cap =
                ArgumentCaptor.forClass(DecisionSynthesisResponse.class);
        verify(decisionService).createFromSynthesis(eq(STARTUP_ID), eq(DEBATE_ID), cap.capture(), any());
        assertThat(cap.getValue().decision()).contains("SYNTH");
    }

    // ---- §11: the blueprint reflects the debate outcome ---------------------

    @Test
    void debateDefersTheCommunityFeedButKeepsTheScanner() throws InterruptedException {
        StartupContext ctx = newContext();
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(ctx, 4.0);

        assertThat(feature(ctx, "Product scanner").isInMvp()).isTrue();
        assertThat(feature(ctx, "Community feed").isInMvp()).isFalse();
    }

    @Test
    void inRealModeTheCeoDeferralListIsAuthoritativeEvenForTheScanner() throws InterruptedException {
        StartupContext ctx = newContext();
        // REAL mode: the CEO synthesis is authoritative. If it names the scanner, the
        // scanner is deferred — no hidden post-processing override protects it. The
        // "keep the scanner" rule survives only as an explicit prompt constraint.
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Product scanner")));
        debate(llm).conduct(ctx, 4.0);

        assertThat(feature(ctx, "Product scanner").isInMvp()).isFalse();
        // Nothing else is force-deferred: the model's scope decision stands exactly.
        assertThat(feature(ctx, "Community feed").isInMvp()).isTrue();
    }

    @Test
    void inScriptedModeTheScannerIsProtectedAndTheCommunityFeedIsCut() throws InterruptedException {
        StartupContext ctx = newContext();
        // SCRIPTED_DEMO: even a real-provider fake makes zero calls — the canned
        // synthesis is ignored and the deterministic synthesis runs, which defers the
        // Community feed while the legacy protection keeps the scanner.
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Product scanner")));
        scriptedDebate(llm).conduct(ctx, 4.0);

        assertThat(llm.structuredCalls).isZero();
        assertThat(feature(ctx, "Product scanner").isInMvp()).isTrue();
        assertThat(feature(ctx, "Community feed").isInMvp()).isFalse();
    }

    @Test
    void inRealModeTheFinanceBudgetIsNotClampedByTheScopeDecision() throws InterruptedException {
        StartupContext ctx = newContext();
        // Simulate the authoritative, LLM-produced Finance budget from Phase 2B.
        ctx.getBudget().setDevelopmentCost(18_000d);
        ctx.getBudget().setMonthlyBurn(4_200d);
        ctx.getStartup().setFinancialHealth(70);

        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(ctx, 4.0);

        // The scope decision recomputes runway but does NOT clamp the LLM's figures to
        // the fixed scripted scope-reduction numbers (14000 / 3900 / 74).
        assertThat(ctx.getBudget().getDevelopmentCost()).isEqualTo(18_000d);
        assertThat(ctx.getBudget().getMonthlyBurn()).isEqualTo(4_200d);
        assertThat(ctx.getStartup().getFinancialHealth()).isEqualTo(70);
    }

    @Test
    void aCleanDebateResolvesWithoutWarnings() throws InterruptedException {
        FakeLLMService llm = fullSequence(synthesis("SYNTH", List.of("Community feed")));
        debate(llm).conduct(newContext(), 4.0);

        verify(debateService).complete(DEBATE_ID, DebateStatus.RESOLVED);
    }

    // ---- §17: a failed turn never crashes the run ---------------------------

    @Test
    void aFailedDebateTurnFallsBackAndMarksTheDebateWithWarnings() throws InterruptedException {
        // Developer's Round-2 call (4th structured call) fails.
        FakeLLMService llm = FakeLLMService.returningSequence(
                dr("DEV_R1"), dr("MKT_R1"), dr("FIN_R1"),
                FakeLLMService.FAILURE, dr("MKT_R2"), dr("FIN_R2"),
                dr("DEV_R3"), dr("MKT_R3"), dr("FIN_R3"),
                synthesis("SYNTH", List.of("Community feed")));

        debate(llm).conduct(newContext(), 4.0);

        // The run still finishes; the debate is closed COMPLETED_WITH_WARNINGS, and a
        // message is still persisted for the failed turn (framing + 9 + finance + synthesis).
        verify(debateService).complete(DEBATE_ID, DebateStatus.COMPLETED_WITH_WARNINGS);
        verify(messageService, times(12)).create(eq(STARTUP_ID), any(AgentType.class),
                anyString(), eq(DEBATE_ID), any(), anyString(), any());
        verify(decisionService).createFromSynthesis(eq(STARTUP_ID), eq(DEBATE_ID), any(), any());
    }

    @Test
    void aFailedSynthesisFallsBackToADeterministicDecision() throws InterruptedException {
        StartupContext ctx = newContext();
        FakeLLMService llm = FakeLLMService.returningSequence(
                dr("DEV_R1"), dr("MKT_R1"), dr("FIN_R1"),
                dr("DEV_R2"), dr("MKT_R2"), dr("FIN_R2"),
                dr("DEV_R3"), dr("MKT_R3"), dr("FIN_R3"),
                FakeLLMService.FAILURE);

        debate(llm).conduct(ctx, 4.0);

        ArgumentCaptor<DecisionSynthesisResponse> cap =
                ArgumentCaptor.forClass(DecisionSynthesisResponse.class);
        verify(decisionService).createFromSynthesis(eq(STARTUP_ID), eq(DEBATE_ID), cap.capture(), any());
        assertThat(cap.getValue().decision()).containsIgnoringCase("focused V1");
        verify(debateService).complete(DEBATE_ID, DebateStatus.COMPLETED_WITH_WARNINGS);
        assertThat(feature(ctx, "Community feed").isInMvp()).isFalse();
    }

    // ---- mock provider: fully deterministic, no LLM calls -------------------

    @Test
    void mockProviderRunsTheWholeDebateWithoutAnyStructuredCall() throws InterruptedException {
        StartupContext ctx = newContext();
        FakeLLMService offline = FakeLLMService.offline();

        debate(offline).conduct(ctx, 4.0);

        assertThat(offline.structuredCalls).isZero();
        verify(decisionService).createFromSynthesis(eq(STARTUP_ID), eq(DEBATE_ID), any(), any());
        verify(debateService).complete(DEBATE_ID, DebateStatus.RESOLVED);
        assertThat(feature(ctx, "Community feed").isInMvp()).isFalse();
        assertThat(feature(ctx, "Product scanner").isInMvp()).isTrue();
    }
}
