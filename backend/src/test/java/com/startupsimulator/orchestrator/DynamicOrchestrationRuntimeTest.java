package com.startupsimulator.orchestrator;

import com.startupsimulator.agent.CeoAgent;
import com.startupsimulator.agent.CeoAnalysisResponse;
import com.startupsimulator.agent.DeveloperAgent;
import com.startupsimulator.agent.DeveloperAnalysisResponse;
import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.agent.FinanceAgent;
import com.startupsimulator.agent.FinanceAnalysisResponse;
import com.startupsimulator.agent.MarketingAgent;
import com.startupsimulator.agent.MarketingAnalysisResponse;
import com.startupsimulator.agent.AgentMemoryContextBuilder;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.memory.MemoryService;
import com.startupsimulator.model.AgentTask;
import com.startupsimulator.model.Debate;
import com.startupsimulator.model.Decision;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.orchestrator.decision.OrchestrationDecision;
import com.startupsimulator.orchestrator.decision.OrchestrationDecisionEngine;
import com.startupsimulator.orchestrator.decision.OrchestrationDecisionValidator;
import com.startupsimulator.orchestrator.decision.OrchestrationStateSnapshotFactory;
import com.startupsimulator.repository.AgentMessageRepository;
import com.startupsimulator.service.AgentInbox;
import com.startupsimulator.service.AgentService;
import com.startupsimulator.service.DebateService;
import com.startupsimulator.service.DecisionService;
import com.startupsimulator.service.EventService;
import com.startupsimulator.service.MessageService;
import com.startupsimulator.service.StartupContextService;
import com.startupsimulator.service.StartupService;
import com.startupsimulator.service.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CA3 Phase 6B — the acceptance proofs that the simulation runtime is genuinely
 * <em>decision-driven</em>: {@code StartupOrchestrator.runDynamicOrchestration}
 * builds a state snapshot, asks the {@link OrchestrationDecisionEngine} what to do
 * next, validates the proposal, and executes exactly that one action — then
 * rebuilds and asks again, with NO fixed CEO→DEV→MKT→FIN order anywhere in the
 * REAL path.
 *
 * <p>The strongest proofs (§22–§25) never inspect the decision DTO: they assert
 * that the chosen agent <b>actually executed</b> (its own {@link FakeLLMService}
 * made exactly one real structured call and the context was mutated) and that the
 * agents the engine did NOT pick made zero calls — and they capture the real
 * execution <b>order</b> from the {@link AgentService#setState} calls the runtime
 * emits, not from the proposals. Failure paths (§26/§27/§30) prove no agent runs
 * and the loop terminates explicitly with no fixed-order fallback; §28 proves the
 * bounded loop stops at {@code MAX_ORCHESTRATION_STEPS}; §31 proves SCRIPTED_DEMO
 * drives the SAME runtime with zero LLM calls.
 *
 * <p>Each agent and the engine get their OWN {@link FakeLLMService} — a shared one
 * would hand the wrong DTO type to the wrong caller. The {@link BoardroomDebate}
 * runs SCRIPTED (zero LLM) so START_DEBATE executes the real boardroom without
 * consuming any agent's canned response. The §29 memory/communication proof lives
 * in {@link DynamicOrchestrationContextTest} (real JPA stores).
 */
class DynamicOrchestrationRuntimeTest {

    private static final long STARTUP = 1L;

    // ---- VALID real-provider responses (captured from the agent unit tests) ----

    private static CeoAnalysisResponse ceoResponse() {
        return new CeoAnalysisResponse(
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
                "Start with the forecasting engine and bank import; defer social features.");
    }

    private static DeveloperAnalysisResponse developerResponse() {
        return new DeveloperAnalysisResponse(
                "Modular monolith: React SPA + Spring Boot API + PostgreSQL.",
                List.of("React", "TypeScript", "Spring Boot", "PostgreSQL"),
                "Weeks 1-6 core engine; Weeks 7-12 scanner and polish.",
                List.of("Model accuracy needs real-world data", "Scope creep from social features"),
                5.0, 80,
                "An AI copilot that turns bank data into a live cash-flow forecast.",
                List.of(new DeveloperAnalysisResponse.FeatureProposal(
                                "Forecasting engine", "Turns bank data into a runway forecast.", true, 4),
                        new DeveloperAnalysisResponse.FeatureProposal(
                                "Bank import", "Secure bank-account import.", true, 3)));
    }

    private static MarketingAnalysisResponse marketingResponse() {
        return new MarketingAnalysisResponse(
                "The fastest runway forecast for freelancers.",
                "Incumbents are generic spreadsheets; niche apps lack personalisation.",
                "Freemium with a $9/mo Pro tier.",
                List.of("Content/SEO", "Short-form video", "Referral loop"),
                "Launch to a freelancer beachhead, drive word-of-mouth, then expand.",
                "freelancers and solo consultants",
                "Know your runway in seconds.",
                "Freemium SaaS with a Pro subscription.",
                List.of("Freemium conversion must clear ~3-5%"));
    }

    private static FinanceAnalysisResponse financeResponse() {
        return new FinanceAnalysisResponse(
                18_000d, 9_000d, 2_000d, 1_500d, 60_000d, 4_200d, 2_500d,
                "Break-even around month 15 at ~900 Pro users.", 70,
                List.of("Runway is tight before revenue ramps"));
    }

    /**
     * Manually wires a real {@link StartupOrchestrator} around real agents (each
     * with its own fake LLM), a real executor, a real decision engine (fed by
     * {@code engineFake}) and thin mocked services. Captures every
     * {@link AgentService#setState} agent argument so a test can read the ACTUAL
     * execution order the runtime produced.
     */
    private static final class Harness {
        final FakeLLMService ceoFake;
        final FakeLLMService devFake;
        final FakeLLMService mktFake;
        final FakeLLMService finFake;
        final FakeLLMService engineFake;

        final AgentService agentService = mock(AgentService.class);
        final EventService eventService = mock(EventService.class);
        final StartupService startupService = mock(StartupService.class);
        final StartupContextService contextService = mock(StartupContextService.class);
        final TaskService taskService = mock(TaskService.class);
        final MessageService messageService = mock(MessageService.class);
        final AgentInbox agentInbox = mock(AgentInbox.class);
        final DebateService debateService = mock(DebateService.class);
        final DecisionService decisionService = mock(DecisionService.class);
        final MemoryService memoryService = mock(MemoryService.class);
        final AgentMessageRepository messageRepository = mock(AgentMessageRepository.class);

        final List<AgentType> executionOrder = new ArrayList<>();
        final StartupOrchestrator orchestrator;
        final StartupContext ctx;

        Harness(FakeLLMService ceoFake, FakeLLMService devFake, FakeLLMService mktFake,
                FakeLLMService finFake, FakeLLMService engineFake,
                LlmProperties agentProps, LlmProperties engineProps) {
            this.ceoFake = ceoFake;
            this.devFake = devFake;
            this.mktFake = mktFake;
            this.finFake = finFake;
            this.engineFake = engineFake;

            CeoAgent ceoAgent = new CeoAgent(ceoFake, agentProps);
            DeveloperAgent developerAgent = new DeveloperAgent(devFake, agentProps);
            MarketingAgent marketingAgent = new MarketingAgent(mktFake, agentProps);
            FinanceAgent financeAgent = new FinanceAgent(finFake, agentProps);

            AgentMemoryContextBuilder memoryContextBuilder =
                    new AgentMemoryContextBuilder(memoryService, eventService);
            OrchestrationDecisionValidator validator = new OrchestrationDecisionValidator();
            OrchestrationStateSnapshotFactory snapshotFactory =
                    new OrchestrationStateSnapshotFactory(memoryContextBuilder, messageRepository);
            OrchestrationDecisionEngine engine =
                    new OrchestrationDecisionEngine(engineFake, engineProps, validator, eventService);
            OrchestrationActionExecutor executor = new OrchestrationActionExecutor(
                    agentService, eventService, agentInbox, memoryContextBuilder,
                    ceoAgent, developerAgent, marketingAgent, financeAgent, scriptedDebate(ceoAgent,
                            developerAgent, marketingAgent, financeAgent));

            // Thin-service stubs. Mockito returns empty collections by default, so
            // the inbox/memory/message stores read as empty unless a test seeds them.
            when(memoryService.findForAgent(anyLong(), any())).thenReturn(List.of());
            when(memoryService.findShared(anyLong())).thenReturn(List.of());
            when(messageRepository.findByStartupIdOrderById(anyLong())).thenReturn(List.of());
            AgentTask task = mock(AgentTask.class);
            when(task.getId()).thenReturn(1L);
            when(taskService.create(anyLong(), any(), any(), any(), any(), any())).thenReturn(task);
            when(agentService.setState(anyLong(), any(AgentType.class), any(), any()))
                    .thenAnswer(inv -> {
                        executionOrder.add(inv.getArgument(1));
                        return null;
                    });

            com.startupsimulator.orchestrator.replan.ReplanEngine replanEngine = mock(com.startupsimulator.orchestrator.replan.ReplanEngine.class);

            this.orchestrator = new StartupOrchestrator(
                    startupService, contextService, agentService, taskService, messageService,
                    agentInbox, debateService, decisionService, eventService,
                    ceoAgent, developerAgent, marketingAgent, financeAgent,
                    scriptedDebate(ceoAgent, developerAgent, marketingAgent, financeAgent),
                    engine, snapshotFactory, executor, replanEngine);
            ReflectionTestUtils.setField(orchestrator, "tickIntervalMs", 0L);

            Startup s = new Startup();
            s.setId(STARTUP);
            s.setName("Acme");
            s.setOriginalIdea("A SaaS tool for small bakeries.");
            this.ctx = new StartupContext(s);
        }

        private BoardroomDebate scriptedDebate(CeoAgent ceo, DeveloperAgent dev,
                                               MarketingAgent mkt, FinanceAgent fin) {
            Debate debate = new Debate(STARTUP, "topic", "question");
            debate.setId(7L);
            when(debateService.create(anyLong(), any(), any())).thenReturn(debate);
            Decision decision = new Decision(STARTUP, 7L, "decision", "reason");
            decision.setId(99L);
            when(decisionService.createFromSynthesis(anyLong(), anyLong(), any(), any()))
                    .thenReturn(decision);
            BoardroomDebate bd = new BoardroomDebate(messageService, debateService, decisionService,
                    eventService, agentService, contextService, startupService,
                    ceo, dev, mkt, fin, FakeLLMService.offline(), new LlmProperties());
            ReflectionTestUtils.setField(bd, "tickIntervalMs", 0L);
            return bd;
        }

        List<AgentType> firstTouchOrder() {
            List<AgentType> order = new ArrayList<>();
            for (AgentType a : executionOrder) {
                if (!order.contains(a)) {
                    order.add(a);
                }
            }
            return order;
        }

        long eventCount(EventType type) {
            ArgumentCaptor<EventType> cap = ArgumentCaptor.forClass(EventType.class);
            verify(eventService, atLeastOnce()).record(anyLong(), cap.capture(), any(), any());
            return cap.getAllValues().stream().filter(type::equals).count();
        }

        // HARNESS-CTOR-END

    }

    private static LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    private static LlmProperties scriptedProps() {
        return new LlmProperties();   // default mode = SCRIPTED_DEMO
    }

    /** Real agents + a REAL decision engine fed by {@code engineFake}. */
    private static Harness realHarness(FakeLLMService engineFake) {
        return new Harness(
                FakeLLMService.returning(ceoResponse()),
                FakeLLMService.returning(developerResponse()),
                FakeLLMService.returning(marketingResponse()),
                FakeLLMService.returning(financeResponse()),
                engineFake, realProps(), realProps());
    }

    // ---- §22 (STRONGEST): the accepted decision controls the ACTUAL next action ----

    @Test
    void t22a_realFinanceDecision_actuallyRunsFinance_devAndMktDoNot() throws Exception {
        Harness h = realHarness(FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "Pricing/revenue model unresolved.",
                "Finance should quantify the unresolved pricing and revenue model next.")));

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        // Finance ACTUALLY executed its real analysis exactly once and mutated state.
        assertThat(h.finFake.structuredCalls).isEqualTo(1);
        assertThat(h.ctx.getBudget().getDevelopmentCost()).isEqualTo(18_000d);
        assertThat(h.ctx.getBudget().getMonthlyBurn()).isEqualTo(4_200d);
        assertThat(h.ctx.getBudget().isAnalysisFailed()).isFalse();
        assertThat(h.ctx.getStartup().getFinancialHealth()).isEqualTo(70);
        // The agents the engine did NOT pick never ran (no fixed CEO→DEV→MKT first).
        assertThat(h.ceoFake.structuredCalls).isZero();
        assertThat(h.devFake.structuredCalls).isZero();
        assertThat(h.mktFake.structuredCalls).isZero();
        assertThat(h.firstTouchOrder()).containsExactly(AgentType.FINANCE);
    }

    @Test
    void t22b_realMarketingDecision_actuallyRunsMarketingFirst_othersDoNot() throws Exception {
        Harness h = realHarness(FakeLLMService.returning(OrchestrationDecision.runAgent(
                "MARKETING", "Positioning must be settled first.",
                "Marketing should establish positioning before the rest proceed.")));

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        assertThat(h.mktFake.structuredCalls).isEqualTo(1);
        assertThat(h.ceoFake.structuredCalls).isZero();
        assertThat(h.devFake.structuredCalls).isZero();
        assertThat(h.finFake.structuredCalls).isZero();
        assertThat(h.firstTouchOrder()).containsExactly(AgentType.MARKETING);
    }

    // ---- §23: the full accepted sequence executes in exactly that order ----

    @Test
    void t23_executesTheAcceptedSequenceInOrder() throws Exception {
        Harness h = realHarness(FakeLLMService.returningSequence(
                OrchestrationDecision.runAgent("FINANCE", "f", "f"),
                OrchestrationDecision.runAgent("DEVELOPMENT", "d", "d"),
                OrchestrationDecision.runAgent("MARKETING", "m", "m"),
                OrchestrationDecision.runAgent("CEO", "c", "c"),
                OrchestrationDecision.startDebate("debate", "all four analyses are in"),
                OrchestrationDecision.completeAnalysis("done", "debate concluded")));

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        assertThat(h.firstTouchOrder()).containsExactly(
                AgentType.FINANCE, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.CEO);
        assertThat(h.finFake.structuredCalls).isEqualTo(1);
        assertThat(h.devFake.structuredCalls).isEqualTo(1);
        assertThat(h.mktFake.structuredCalls).isEqualTo(1);
        assertThat(h.ceoFake.structuredCalls).isEqualTo(1);
        assertThat(h.ctx.getStartup().isSimulationCompleted()).isTrue();
    }

    // ---- §24: a DIFFERENT accepted sequence changes the execution order ----

    @Test
    void t24_differentAcceptedSequence_changesExecutionOrder() throws Exception {
        Harness h = realHarness(FakeLLMService.returningSequence(
                OrchestrationDecision.runAgent("MARKETING", "m", "m"),
                OrchestrationDecision.runAgent("FINANCE", "f", "f"),
                OrchestrationDecision.runAgent("CEO", "c", "c"),
                OrchestrationDecision.runAgent("DEVELOPMENT", "d", "d"),
                OrchestrationDecision.startDebate("debate", "all four analyses are in"),
                OrchestrationDecision.completeAnalysis("done", "debate concluded")));

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        assertThat(h.firstTouchOrder()).containsExactly(
                AgentType.MARKETING, AgentType.FINANCE, AgentType.CEO, AgentType.DEVELOPMENT);
        assertThat(h.ctx.getStartup().isSimulationCompleted()).isTrue();
    }

    // ---- §25: same initial state, different decisions → different execution ----

    @Test
    void t25_sameInitialState_differentDecisions_yieldDifferentExecution() throws Exception {
        Harness runA = realHarness(FakeLLMService.returningSequence(
                OrchestrationDecision.runAgent("FINANCE", "f", "f"),
                OrchestrationDecision.runAgent("DEVELOPMENT", "d", "d")));
        Harness runB = realHarness(FakeLLMService.returningSequence(
                OrchestrationDecision.runAgent("MARKETING", "m", "m"),
                OrchestrationDecision.runAgent("FINANCE", "f", "f")));

        runA.orchestrator.runDynamicOrchestration(runA.ctx, 1.0);
        runB.orchestrator.runDynamicOrchestration(runB.ctx, 1.0);

        // After the two accepted RUN_AGENTs each run exhausts its sequence; the reused
        // final decision targets an already-complete agent → rejected → safe stop.
        assertThat(runA.firstTouchOrder()).containsExactly(AgentType.FINANCE, AgentType.DEVELOPMENT);
        assertThat(runB.firstTouchOrder()).containsExactly(AgentType.MARKETING, AgentType.FINANCE);
        assertThat(runA.firstTouchOrder()).isNotEqualTo(runB.firstTouchOrder());
    }

    // ---- §26: an invalid RUN_AGENT target executes NOTHING, stops explicitly ----

    @Test
    void t26_invalidAgentTarget_runsNothing_terminatesWithNoFallback() throws Exception {
        Harness h = realHarness(FakeLLMService.returning(OrchestrationDecision.runAgent(
                "ACCOUNTANT", "mystery role", "an agent that does not exist")));

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        // No agent ran at all — and crucially NO fixed-order fallback kicked in.
        assertThat(h.ceoFake.structuredCalls).isZero();
        assertThat(h.devFake.structuredCalls).isZero();
        assertThat(h.mktFake.structuredCalls).isZero();
        assertThat(h.finFake.structuredCalls).isZero();
        assertThat(h.firstTouchOrder()).isEmpty();
        assertThat(h.ctx.getStartup().isSimulationCompleted()).isFalse();
        assertThat(h.eventCount(EventType.ORCHESTRATION_RUNTIME_TERMINATED)).isEqualTo(1);
    }

    // ---- §27: a repeated RUN_AGENT after that agent completed is rejected, loop stops --

    @Test
    void t27_repeatedNonProgressingDecision_isRejected_loopTerminatesSafely() throws Exception {
        Harness h = realHarness(FakeLLMService.returningSequence(
                OrchestrationDecision.runAgent("FINANCE", "f", "first — legal"),
                OrchestrationDecision.runAgent("FINANCE", "f", "again — Finance already done")));

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        // Finance ran exactly ONCE (not twice): the second proposal was rejected,
        // not executed — proving no infinite loop and no false re-progression.
        assertThat(h.finFake.structuredCalls).isEqualTo(1);
        assertThat(h.firstTouchOrder()).containsExactly(AgentType.FINANCE);
        assertThat(h.eventCount(EventType.ORCHESTRATION_RUNTIME_TERMINATED)).isEqualTo(1);
    }

    // ---- §28: MAX_ORCHESTRATION_STEPS bounds the loop (no auto-finalize) ----

    @Test
    void t28_maxSteps_boundsTheLoop_withoutFinalizing() throws Exception {
        // Finance always FAILS, so it never completes and stays pending; the engine
        // keeps (legally) proposing it. The bound — not an infinite loop — stops us.
        Harness h = new Harness(
                FakeLLMService.returning(ceoResponse()),
                FakeLLMService.returning(developerResponse()),
                FakeLLMService.returning(marketingResponse()),
                FakeLLMService.failing(),
                FakeLLMService.returning(OrchestrationDecision.runAgent("FINANCE", "f", "retry")),
                realProps(), realProps());

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        assertThat(h.finFake.structuredCalls).isEqualTo(StartupOrchestrator.MAX_ORCHESTRATION_STEPS);
        assertThat(h.ctx.getStartup().isSimulationCompleted()).isFalse();
        assertThat(h.eventCount(EventType.ORCHESTRATION_RUNTIME_TERMINATED)).isEqualTo(1);
    }

    // ---- §30: a REAL LLM failure on a SUBSEQUENT iteration stops explicitly ----

    @Test
    void t30_llmFailureOnLaterIteration_stopsExplicitly_noFallback() throws Exception {
        Harness h = realHarness(FakeLLMService.returningSequence(
                OrchestrationDecision.runAgent("FINANCE", "f", "legal first step"),
                FakeLLMService.FAILURE));   // the SECOND decideNext throws

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        // Finance ran once; then the engine failed and the runtime stopped — it did
        // NOT fabricate a decision or revert to CEO→DEV→MKT→FIN.
        assertThat(h.finFake.structuredCalls).isEqualTo(1);
        assertThat(h.ceoFake.structuredCalls).isZero();
        assertThat(h.devFake.structuredCalls).isZero();
        assertThat(h.mktFake.structuredCalls).isZero();
        assertThat(h.firstTouchOrder()).containsExactly(AgentType.FINANCE);
        assertThat(h.ctx.getStartup().isSimulationCompleted()).isFalse();
        assertThat(h.eventCount(EventType.ORCHESTRATION_RUNTIME_TERMINATED)).isEqualTo(1);
    }

    // ---- §31: SCRIPTED_DEMO drives the SAME runtime with ZERO LLM calls ----

    @Test
    void t31_scriptedDemo_deterministic_zeroLlm_sameRuntime_terminatesNormally() throws Exception {
        Harness h = new Harness(
                FakeLLMService.returning(ceoResponse()),
                FakeLLMService.returning(developerResponse()),
                FakeLLMService.returning(marketingResponse()),
                FakeLLMService.returning(financeResponse()),
                FakeLLMService.offline(),
                scriptedProps(), scriptedProps());

        h.orchestrator.runDynamicOrchestration(h.ctx, 1.0);

        // Zero real LLM calls anywhere (engine deterministic; agents deterministic).
        assertThat(h.engineFake.structuredCalls).isZero();
        assertThat(h.ceoFake.structuredCalls).isZero();
        assertThat(h.devFake.structuredCalls).isZero();
        assertThat(h.mktFake.structuredCalls).isZero();
        assertThat(h.finFake.structuredCalls).isZero();
        // Deterministic order through the identical runtime, ending in normal completion.
        assertThat(h.firstTouchOrder()).containsExactly(
                AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE);
        assertThat(h.ctx.getStartup().isSimulationCompleted()).isTrue();
    }
}
