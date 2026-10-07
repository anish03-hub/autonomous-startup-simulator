package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.model.enums.SimulationPhase;
import com.startupsimulator.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CA3 Phase 6A — the headline proof that orchestration is now DYNAMIC: the next
 * step is genuinely chosen by the model from a bounded state, Java only validates,
 * and there is NO hidden fixed-order fallback in REAL mode. These are plain unit
 * tests: a {@link FakeLLMService} returns a controlled structured decision (no
 * network), the real {@link OrchestrationDecisionValidator} judges it, and the
 * snapshot is built directly so each state is deterministic.
 *
 * <p>The {@code DataJpaTest}-backed proofs that memory and real agent messages
 * actually enter the orchestration prompt live in
 * {@link OrchestrationDecisionContextTest}.
 */
class OrchestrationDecisionEngineTest {

    private static final long STARTUP = 7L;

    private EventService events;
    private OrchestrationDecisionValidator validator;

    @BeforeEach
    void setUp() {
        events = mock(EventService.class);
        validator = new OrchestrationDecisionValidator();
    }

    private OrchestrationDecisionEngine engine(FakeLLMService llm, LlmProperties props) {
        return new OrchestrationDecisionEngine(llm, props, validator, events);
    }

    private static LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    private static LlmProperties scriptedProps() {
        return new LlmProperties();   // default SCRIPTED_DEMO
    }

    // ---- §17: a message Developer→Finance, the LLM picks FINANCE, Java accepts --
    @Test
    void t17_dynamicFinanceSelection_isLlmProposed_andAccepted() {
        AgentMessage devToFinance = new AgentMessage(STARTUP, AgentType.DEVELOPMENT, AgentType.FINANCE,
                "Cost validation", "Implementation cost assumptions need financial validation.");
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .currentPhase(SimulationPhase.ANALYSIS)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING)
                .pending(AgentType.FINANCE)
                .messages(List.of(devToFinance))
                .build();

        // The decision is the MODEL'S — not hard-coded in Java.
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "Finance should validate the cost assumptions.",
                "Development explicitly asked Finance to validate implementation cost assumptions."));

        OrchestrationDecisionOutcome out = engine(fake, realProps()).decideNext(snapshot);

        assertThat(fake.structuredCalls).isEqualTo(1);     // a genuine LLM reasoning call
        assertThat(out.isAccepted()).isTrue();
        assertThat(out.action()).isEqualTo(OrchestrationAction.RUN_AGENT);
        assertThat(out.resolvedAgent()).isEqualTo(AgentType.FINANCE);
        assertThat(out.usedRealLlm()).isTrue();
        verify(events).record(eq(STARTUP), eq(EventType.ORCHESTRATION_DECISION_ACCEPTED), anyString(), anyMap());
    }

    // ---- §18: SAME legal state, DIFFERENT LLM response → DIFFERENT accepted agent.
    //       The only thing that changes is the model's answer, which proves there is
    //       no fixed Java ordering choosing the agent.
    @Test
    void t18_sameState_differentLlmResponse_yieldsDifferentDecision() {
        // Identical state: CEO + Marketing done; Development AND Finance both pending/legal.
        java.util.function.Supplier<OrchestrationStateSnapshot> state = () ->
                OrchestrationStateSnapshot.builder(STARTUP)
                        .currentPhase(SimulationPhase.ANALYSIS)
                        .completed(AgentType.CEO, AgentType.MARKETING)
                        .pending(AgentType.DEVELOPMENT, AgentType.FINANCE)
                        .build();

        // State-A reading: a technical blocker → the model picks DEVELOPMENT.
        FakeLLMService pickDev = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "DEVELOPMENT", "Resolve the technical blocker first.", "A technical blocker is open."));
        OrchestrationDecisionOutcome a = engine(pickDev, realProps()).decideNext(state.get());

        // State-B reading: pricing unresolved → the model picks FINANCE instead.
        FakeLLMService pickFinance = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "Pricing is unresolved.", "Revenue model is still open."));
        OrchestrationDecisionOutcome b = engine(pickFinance, realProps()).decideNext(state.get());

        assertThat(a.isAccepted()).isTrue();
        assertThat(a.resolvedAgent()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(b.isAccepted()).isTrue();
        assertThat(b.resolvedAgent()).isEqualTo(AgentType.FINANCE);
        // Same state in, different agent out: the choice tracked the LLM, not Java.
        assertThat(a.resolvedAgent()).isNotEqualTo(b.resolvedAgent());
    }

    // ---- §19: an invalid agent is rejected explicitly — never coerced, no run ----
    @Test
    void t19_invalidAgent_isRejected_noExecution_noFallback() {
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING)
                .pending(AgentType.FINANCE)
                .build();
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "ACCOUNTANT", "Run the accountant.", "No such department exists."));

        OrchestrationDecisionOutcome out = engine(fake, realProps()).decideNext(snapshot);

        assertThat(out.isAccepted()).isFalse();
        assertThat(out.status()).isEqualTo(OrchestrationDecisionOutcome.REJECTED);
        assertThat(out.resolvedAgent()).isNull();          // nothing coerced to FINANCE/DEVELOPMENT
        assertThat(out.detail()).contains("ACCOUNTANT").contains("not a valid agent");
        verify(events).record(eq(STARTUP), eq(EventType.ORCHESTRATION_DECISION_REJECTED), anyString(), anyMap());
        verify(events, never()).record(eq(STARTUP), eq(EventType.ORCHESTRATION_DECISION_ACCEPTED), anyString(), anyMap());
    }

    // ---- §20: an action illegal for the current state is rejected with a reason --
    @Test
    void t20_illegalStartDebate_isRejected_withExplicitReason() {
        // Debate preconditions NOT met: only two analyses are complete.
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT)
                .pending(AgentType.MARKETING, AgentType.FINANCE)
                .build();
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.startDebate(
                "Convene the debate now.", "Jumping ahead."));

        OrchestrationDecisionOutcome out = engine(fake, realProps()).decideNext(snapshot);

        assertThat(out.isAccepted()).isFalse();
        assertThat(out.status()).isEqualTo(OrchestrationDecisionOutcome.REJECTED);
        assertThat(out.detail()).contains("START_DEBATE").contains("not legal");
        verify(events).record(eq(STARTUP), eq(EventType.ORCHESTRATION_DECISION_REJECTED), anyString(), anyMap());
    }

    // ---- §23: only the configured maximum of previous decisions enters the prompt
    @Test
    void t23_previousDecisions_areBoundedInThePrompt() {
        List<OrchestrationDecision> history = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            history.add(OrchestrationDecision.runAgent(
                    "CEO", "history-reason-" + i, "history-rationale-" + i));
        }
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING)
                .pending(AgentType.FINANCE)
                .previousDecisions(history)
                .build();
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "next", "next"));

        engine(fake, realProps()).decideNext(snapshot);

        // 7 provided, MAX_ORCHESTRATION_HISTORY = 5 → the oldest two are dropped.
        assertThat(OrchestrationDecisionEngine.MAX_ORCHESTRATION_HISTORY).isEqualTo(5);
        assertThat(fake.lastUserPrompt)
                .doesNotContain("history-reason-1")
                .doesNotContain("history-reason-2")
                .contains("history-reason-3")
                .contains("history-reason-4")
                .contains("history-reason-5")
                .contains("history-reason-6")
                .contains("history-reason-7");
    }

    // ---- §24: a REAL-mode LLM failure is EXPLICIT — no deterministic substitution
    @Test
    void t24_realModeLlmFailure_isExplicit_noHiddenFallback() {
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO)
                .pending(AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE)
                .build();
        FakeLLMService fake = FakeLLMService.failing();   // generateStructured throws

        OrchestrationDecisionOutcome out = engine(fake, realProps()).decideNext(snapshot);

        assertThat(fake.structuredCalls).isEqualTo(1);
        assertThat(out.status()).isEqualTo(OrchestrationDecisionOutcome.LLM_FAILURE);
        assertThat(out.failed()).isTrue();
        assertThat(out.usedRealLlm()).isTrue();
        // Nothing is fabricated: no decision, no action, no agent is substituted.
        assertThat(out.decision()).isNull();
        assertThat(out.action()).isNull();
        assertThat(out.resolvedAgent()).isNull();
        verify(events).record(eq(STARTUP), eq(EventType.ORCHESTRATION_DECISION_REJECTED), anyString(), anyMap());
        verify(events, never()).record(eq(STARTUP), eq(EventType.ORCHESTRATION_DECISION_ACCEPTED), anyString(), anyMap());
    }

    // ---- §24 (variant): REAL mode but no real provider → explicit, no fabrication
    @Test
    void t24b_realModeWithoutProvider_producesNoDecision() {
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO)
                .pending(AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE)
                .build();
        FakeLLMService offline = FakeLLMService.offline();   // isRealProvider() == false

        OrchestrationDecisionOutcome out = engine(offline, realProps()).decideNext(snapshot);

        assertThat(offline.structuredCalls).isZero();        // never even attempted a call
        assertThat(out.status()).isEqualTo(OrchestrationDecisionOutcome.NO_REAL_PROVIDER);
        assertThat(out.decision()).isNull();
        assertThat(out.resolvedAgent()).isNull();
    }

    // ---- §25: SCRIPTED_DEMO is deterministic, valid, and makes ZERO LLM calls ----
    @Test
    void t25_scriptedDemo_isDeterministic_andNeverCallsTheLlm() {
        OrchestrationStateSnapshot runNext = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO)
                .pending(AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE)
                .build();
        // A real-looking fake, to prove the scripted path never touches it.
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent("FINANCE", "x", "x"));

        OrchestrationDecisionOutcome out = engine(fake, scriptedProps()).decideNext(runNext);

        assertThat(fake.structuredCalls).isZero();           // §13: zero real LLM calls
        assertThat(out.isAccepted()).isTrue();
        assertThat(out.usedRealLlm()).isFalse();
        // Deterministic policy: the first pending department.
        assertThat(out.resolvedAgent()).isEqualTo(AgentType.DEVELOPMENT);
    }

    // ---- §25 (coverage): the scripted policy also convenes the debate / completes
    @Test
    void t25b_scriptedDemo_startsDebateThenCompletes_deterministically() {
        FakeLLMService fake = FakeLLMService.offline();

        OrchestrationStateSnapshot allDone = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE)
                .build();
        OrchestrationDecisionOutcome debate = engine(fake, scriptedProps()).decideNext(allDone);
        assertThat(debate.isAccepted()).isTrue();
        assertThat(debate.action()).isEqualTo(OrchestrationAction.START_DEBATE);

        OrchestrationStateSnapshot debated = OrchestrationStateSnapshot.builder(STARTUP)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE)
                .debateStarted(true).debateComplete(true)
                .build();
        OrchestrationDecisionOutcome complete = engine(fake, scriptedProps()).decideNext(debated);
        assertThat(complete.isAccepted()).isTrue();
        assertThat(complete.action()).isEqualTo(OrchestrationAction.COMPLETE_ANALYSIS);

        assertThat(fake.structuredCalls).isZero();
    }

    // ---- §6: the prompt is built from the seven bounded, labelled sections -------
    @Test
    void tPrompt_rendersTheSevenLabelledSections_andLegalActions() {
        OrchestrationStateSnapshot snapshot = OrchestrationStateSnapshot.builder(STARTUP)
                .currentPhase(SimulationPhase.ANALYSIS)
                .completed(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING)
                .pending(AgentType.FINANCE)
                .analysis(AgentType.CEO, "Target bakeries with a scanner-first MVP.")
                .build();
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent("FINANCE", "x", "x"));

        engine(fake, realProps()).decideNext(snapshot);

        assertThat(fake.lastUserPrompt)
                .contains("=== STARTUP CONTEXT ===")
                .contains("=== COMPLETED AGENT ANALYSES ===")
                .contains("=== RECENT AGENT MESSAGES ===")
                .contains("=== RELEVANT PERSISTENT MEMORY ===")
                .contains("=== CURRENT SIMULATION STATE ===")
                .contains("=== AVAILABLE NEXT ACTIONS ===")
                .contains("=== PREVIOUS ORCHESTRATION DECISIONS ===")
                .contains("RUN_AGENT with targetAgent in FINANCE");   // only the legal target offered
    }
}
