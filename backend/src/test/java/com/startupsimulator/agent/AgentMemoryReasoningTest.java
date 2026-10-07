package com.startupsimulator.agent;

import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.memory.AgentMemory;
import com.startupsimulator.memory.AgentMemoryRepository;
import com.startupsimulator.memory.CreateMemoryRequest;
import com.startupsimulator.memory.MemoryRecord;
import com.startupsimulator.memory.MemoryScope;
import com.startupsimulator.memory.MemoryService;
import com.startupsimulator.memory.MemoryType;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.service.EventService;
import com.startupsimulator.tool.ToolExecutionService;
import com.startupsimulator.tool.ToolRegistry;
import com.startupsimulator.tool.impl.DevelopmentEffortEstimatorTool;
import com.startupsimulator.tool.impl.FinancialCalculatorTool;
import com.startupsimulator.tool.impl.PricingRevenueCalculatorTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * CA3 Phase 5B — agent memory creation, retrieval and reasoning integration,
 * proven against the real embedded H2 database and the real Phase 5A
 * {@link MemoryService} (no in-memory Java pass-through, no mocked store). The
 * only test double for the store is a mocked {@link EventService} (events are
 * infrastructure, not the persistence boundary) and the {@link FakeLLMService}
 * seam (so no network/real provider is ever used).
 *
 * <p>The headline proof (T10 / e2e / §12 / §19): a memory an agent intentionally
 * creates in RUN 1 is persisted to the DB, and a <b>fresh</b> service, builder,
 * agent and context — same startup identity — retrieve it and inject it into the
 * fresh agent's reasoning prompt in RUN 2.
 */
@DataJpaTest
class AgentMemoryReasoningTest {

    private static final long STARTUP = 1L;

    @Autowired
    private AgentMemoryRepository repository;

    @Autowired
    private TestEntityManager em;

    private EventService events;
    private MemoryService memoryService;
    private AgentMemoryContextBuilder memory;

    @BeforeEach
    void setUp() {
        events = mock(EventService.class);
        memoryService = new MemoryService(repository, events);
        memory = new AgentMemoryContextBuilder(memoryService, events);
    }

    // ---- helpers ------------------------------------------------------------

    private static StartupContext newContext() {
        Startup s = new Startup();
        s.setId(STARTUP);
        s.setName("Acme");
        s.setOriginalIdea("A SaaS tool for small bakeries.");
        return new StartupContext(s);
    }

    private static LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** Default (SCRIPTED_DEMO) properties — the deterministic, zero-LLM path. */
    private static LlmProperties scriptedProps() {
        return new LlmProperties();
    }

    /** A valid Development analysis carrying the given memory intents (canonical ctor). */
    private static DeveloperAnalysisResponse devResponse(List<AgentMemoryIntent> intents) {
        return new DeveloperAnalysisResponse(
                "Modular monolith: React SPA + Spring Boot API + PostgreSQL.",
                List.of("React", "Spring Boot", "PostgreSQL"),
                "12 weeks across two phases.",
                List.of("Scanner accuracy needs real-world testing."),
                4.0, 80, "A scanner-first bakery SaaS.",
                List.of(), null, intents);
    }

    /** A valid Finance analysis carrying the given memory intents (canonical ctor). */
    private static FinanceAnalysisResponse financeResponse(List<AgentMemoryIntent> intents) {
        return new FinanceAnalysisResponse(
                20_000d, 10_000d, 2_000d, 1_600d, 60_000d, 4_000d, 3_000d,
                "Break-even around month 14.", 70, List.of(), null, intents);
    }

    private MemoryRecord savePrivate(AgentType agent, MemoryType type, int importance, String content) {
        return memoryService.save(new CreateMemoryRequest(
                STARTUP, agent, MemoryScope.AGENT_PRIVATE, type, content, importance, "seed", null));
    }

    private MemoryRecord saveShared(AgentType author, MemoryType type, int importance, String content) {
        return memoryService.save(new CreateMemoryRequest(
                STARTUP, author, MemoryScope.STARTUP_SHARED, type, content, importance, "seed", null));
    }

    private static final String HEADER = "=== RELEVANT PERSISTENT MEMORY ===";
    private static final String NONE = "No relevant persistent memory was found.";

    // ---- T1: no memory → explicit empty section, nothing fabricated ----------
    @Test
    void t1_noMemory_rendersExplicitNoneSection_andFabricatesNothing() {
        String block = memory.relevantMemoryBlock(STARTUP, AgentType.DEVELOPMENT);

        assertThat(block).contains(HEADER).contains(NONE);
        assertThat(repository.count()).isZero();   // retrieval never invents a row
    }

    // ---- T2: an agent's private memory is retrieved into its prompt ----------
    @Test
    void t2_privateMemory_entersTheAgentsPrompt() {
        savePrivate(AgentType.DEVELOPMENT, MemoryType.DECISION, 7,
                "Chose a modular monolith over microservices.");
        em.flush();
        em.clear();

        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of()));
        new DeveloperAgent(fake, realProps()).runAnalysisWithMemory(newContext(), memory);

        assertThat(fake.lastUserPrompt)
                .contains(HEADER)
                .contains("Chose a modular monolith over microservices.");
    }

    // ---- T3: shared memory is retrieved by multiple different agents ---------
    @Test
    void t3_sharedMemory_isRetrievedByMultipleAgents() {
        saveShared(AgentType.CEO, MemoryType.FACT, 8, "Target users are small independent bakeries.");
        em.flush();
        em.clear();

        FakeLLMService f1 = FakeLLMService.returning(devResponse(List.of()));
        new DeveloperAgent(f1, realProps()).runAnalysisWithMemory(newContext(), memory);
        assertThat(f1.lastUserPrompt).contains("Target users are small independent bakeries.");

        FakeLLMService f2 = FakeLLMService.returning(financeResponse(List.of()));
        new FinanceAgent(f2, realProps()).runAnalysisWithMemory(newContext(), memory);
        assertThat(f2.lastUserPrompt).contains("Target users are small independent bakeries.");
    }

    // ---- T4: one agent's private memory never leaks to another agent ---------
    @Test
    void t4_privateMemory_isIsolatedPerAgent() {
        savePrivate(AgentType.DEVELOPMENT, MemoryType.PREFERENCE, 6, "Developer-only note: prefer PostgreSQL.");
        em.flush();
        em.clear();

        FakeLLMService fake = FakeLLMService.returning(financeResponse(List.of()));
        new FinanceAgent(fake, realProps()).runAnalysisWithMemory(newContext(), memory);

        assertThat(fake.lastUserPrompt)
                .doesNotContain("Developer-only note")
                .contains(NONE);   // Finance sees no private + no shared memory
    }

    // ---- T5: ranking is deterministic and bounded (private>shared, importance desc)
    @Test
    void t5_selection_isRankedAndBoundedToFive() {
        savePrivate(AgentType.DEVELOPMENT, MemoryType.DECISION, 9, "p-nine");
        savePrivate(AgentType.DEVELOPMENT, MemoryType.DECISION, 3, "p-three");
        savePrivate(AgentType.DEVELOPMENT, MemoryType.DECISION, 7, "p-seven");
        saveShared(AgentType.CEO, MemoryType.FACT, 10, "s-ten");
        saveShared(AgentType.CEO, MemoryType.FACT, 8, "s-eight");
        saveShared(AgentType.CEO, MemoryType.FACT, 6, "s-six");
        saveShared(AgentType.CEO, MemoryType.FACT, 5, "s-five");   // 3 private + 4 shared = 7
        em.flush();
        em.clear();

        List<MemoryRecord> sel = memory.selectRelevant(STARTUP, AgentType.DEVELOPMENT);

        assertThat(sel).hasSize(AgentMemoryContextBuilder.MAX_RELEVANT_MEMORIES);   // capped at 5
        // All private memories rank before any shared memory, regardless of importance.
        assertThat(sel.subList(0, 3)).allMatch(r -> r.scope() == MemoryScope.AGENT_PRIVATE);
        assertThat(sel.get(0).content()).isEqualTo("p-nine");    // private, importance desc
        assertThat(sel.get(1).content()).isEqualTo("p-seven");
        assertThat(sel.get(2).content()).isEqualTo("p-three");
        assertThat(sel.get(3).scope()).isEqualTo(MemoryScope.STARTUP_SHARED);
        assertThat(sel.get(3).content()).isEqualTo("s-ten");     // shared, importance desc
        assertThat(sel.get(4).content()).isEqualTo("s-eight");
    }

    // ---- T6: an LLM-driven memory intent is validated and persisted ----------
    @Test
    void t6_agentMemoryIntent_isPersistedAfterSuccessfulAnalysis() {
        AgentMemoryIntent intent = new AgentMemoryIntent(
                "AGENT_PRIVATE", "DECISION", "Committed to a scanner-first MVP.", 8);
        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of(intent)));

        AgentAnalysisOutcome out = new DeveloperAgent(fake, realProps())
                .runAnalysisWithMemory(newContext(), memory);

        assertThat(out.failed()).isFalse();
        em.flush();
        em.clear();
        List<AgentMemory> rows = repository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getContent()).isEqualTo("Committed to a scanner-first MVP.");
        assertThat(rows.get(0).getAgentType()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(rows.get(0).getMemoryType()).isEqualTo(MemoryType.DECISION);
    }

    // ---- T7: no intents → nothing is auto-saved (no "save every response") ----
    @Test
    void t7_noIntents_persistsNothing() {
        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of()));

        new DeveloperAgent(fake, realProps()).runAnalysisWithMemory(newContext(), memory);

        em.flush();
        em.clear();
        assertThat(repository.count()).isZero();
    }

    // ---- T8: identical intents de-duplicate to a single row -------------------
    @Test
    void t8_duplicateIntents_createOnlyOneRow() {
        AgentMemoryIntent dup = new AgentMemoryIntent("AGENT_PRIVATE", "FACT", "Bakeries value speed.", 6);
        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of(dup, dup)));

        new DeveloperAgent(fake, realProps()).runAnalysisWithMemory(newContext(), memory);

        em.flush();
        em.clear();
        assertThat(repository.count()).isEqualTo(1);   // Phase 5A dedup remains intact
    }

    // ---- T9: an intent naming a type outside the vocabulary creates no row ----
    @Test
    void t9_invalidIntent_isSkipped_noRow() {
        AgentMemoryIntent bad = new AgentMemoryIntent(
                "AGENT_PRIVATE", "NONSENSE_TYPE", "Should not persist.", 5);
        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of(bad)));

        new DeveloperAgent(fake, realProps()).runAnalysisWithMemory(newContext(), memory);

        em.flush();
        em.clear();
        assertThat(repository.count()).isZero();
    }

    // ---- T10: a memory survives into a fresh service instance (via the DB) ----
    @Test
    void t10_memory_isRetrievableThroughAFreshServiceInstance() {
        saveShared(AgentType.DEVELOPMENT, MemoryType.INSIGHT, 7, "Scanner accuracy drives retention.");
        em.flush();
        em.clear();

        // A different service/builder object graph — proving the store, not an object field.
        MemoryService fresh = new MemoryService(repository, mock(EventService.class));
        AgentMemoryContextBuilder freshBuilder = new AgentMemoryContextBuilder(fresh, mock(EventService.class));

        List<MemoryRecord> sel = freshBuilder.selectRelevant(STARTUP, AgentType.DEVELOPMENT);
        assertThat(sel).hasSize(1);
        assertThat(sel.get(0).content()).isEqualTo("Scanner accuracy drives retention.");
    }

    // ---- T11: memory materially enters the reasoning prompt, framed as evidence
    @Test
    void t11_memory_materiallyEntersReasoning() {
        savePrivate(AgentType.DEVELOPMENT, MemoryType.LESSON, 7, "Last pivot failed due to scope creep.");
        em.flush();
        em.clear();

        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of()));
        new DeveloperAgent(fake, realProps()).runAnalysisWithMemory(newContext(), memory);

        assertThat(fake.lastUserPrompt)
                .contains("Last pivot failed due to scope creep.")
                .contains("not unquestionable");   // §15 framing: evidence, prefer current context
    }

    // ---- T12: memory and communication are both present and kept distinct -----
    @Test
    void t12_memoryAndCommunication_areDistinctSections() {
        StartupContext ctx = newContext();
        ctx.deliverInbox(AgentType.FINANCE, List.of(new AgentMessage(
                STARTUP, AgentType.DEVELOPMENT, AgentType.FINANCE,
                "Scope", "Five months of build at full scope.")));
        savePrivate(AgentType.FINANCE, MemoryType.RISK, 8, "Runway risk under six months.");
        em.flush();
        em.clear();

        FakeLLMService fake = FakeLLMService.returning(financeResponse(List.of()));
        new FinanceAgent(fake, realProps()).runAnalysisWithMemory(ctx, memory);

        assertThat(fake.lastUserPrompt)
                .contains("=== MESSAGES ADDRESSED TO YOU ===")         // Phase 3 (section C)
                .contains("Five months of build at full scope.")
                .contains(HEADER)                                       // Phase 5B (section E)
                .contains("Runway risk under six months.");
    }

    // ---- T13: memory + genuine tool use through the ACTUAL ToolExecutionService
    @Test
    void t13_memory_thenRealToolExecution_bothFlowThroughReasoning() {
        ToolRegistry registry = new ToolRegistry(List.of(
                new FinancialCalculatorTool(),
                new DevelopmentEffortEstimatorTool(),
                new PricingRevenueCalculatorTool()));
        ToolExecutionService exec = new ToolExecutionService(registry, mock(EventService.class));
        AgentToolReasoner reasoner = new AgentToolReasoner(registry, exec);   // the real tool pipeline

        savePrivate(AgentType.FINANCE, MemoryType.PREFERENCE, 7, "Keep infrastructure under $2k per month.");
        em.flush();
        em.clear();

        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "cash maths", Map.of(
                        "developmentCost", 10000, "infrastructureCost", 2000,
                        "marketingBudget", 5000, "operatingCost", 1500,
                        "startingCapital", 60000, "monthlyExpenses", 4000)),
                AgentToolStep.finalAnswer("Runway looks healthy."));

        FinanceAgent fin = new FinanceAgent(fake, realProps());
        ToolAssistedOutcome out = fin.reasonWithToolsAndMemory(newContext(), reasoner, memory);

        assertThat(out.toolExecutions()).isEqualTo(1);
        // Memory is in the FIRST reasoning prompt (retrieved once, up front)...
        assertThat(fake.userPrompts.get(0))
                .contains(HEADER)
                .contains("Keep infrastructure under $2k per month.");
        // ...and the ACTUAL tool's real figure is in the NEXT prompt, as a separate section.
        assertThat(fake.userPrompts.get(1))
                .contains("=== TOOL INTERACTION HISTORY ===")
                .contains("18500.0");
        // Retrieval happened exactly once for the whole bounded loop (not per tool call).
        verify(events, times(1)).record(eq(STARTUP), eq(EventType.MEMORY_RETRIEVED), anyString(), anyMap());
    }

    // ---- T14: REAL mode preserved — one structured call, intent persists ------
    @Test
    void t14_realMode_runsViaProvider_andPersistsIntent() {
        AgentMemoryIntent intent = new AgentMemoryIntent(
                "STARTUP_SHARED", "DECISION", "Ship V1 without the community feed.", 8);
        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of(intent)));

        AgentAnalysisOutcome out = new DeveloperAgent(fake, realProps())
                .runAnalysisWithMemory(newContext(), memory);

        assertThat(fake.structuredCalls).isEqualTo(1);   // exactly one real structured call
        assertThat(out.usedRealLlm()).isTrue();
        assertThat(out.failed()).isFalse();
        em.flush();
        em.clear();
        List<AgentMemory> rows = repository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getScope()).isEqualTo(MemoryScope.STARTUP_SHARED);
    }

    // ---- T15: SCRIPTED_DEMO stays deterministic — no LLM call, no memory ------
    @Test
    void t15_scriptedDemo_makesNoLlmCall_andCreatesNoMemory() {
        // Even though the (ignored) canned response carries an intent, the scripted
        // path never reads it and never queues an intent.
        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of(
                new AgentMemoryIntent("AGENT_PRIVATE", "DECISION", "should not persist", 5))));

        AgentAnalysisOutcome out = new DeveloperAgent(fake, scriptedProps())
                .runAnalysisWithMemory(newContext(), memory);

        assertThat(fake.structuredCalls).isZero();   // zero real LLM calls in SCRIPTED_DEMO
        assertThat(out.usedRealLlm()).isFalse();
        em.flush();
        em.clear();
        assertThat(repository.count()).isZero();      // deterministic path persists nothing
    }

    // ---- T16: the plain (non-memory) path is unchanged — no memory section ----
    @Test
    void t16_plainPath_hasNoMemorySection() {
        savePrivate(AgentType.DEVELOPMENT, MemoryType.FACT, 7, "This must not appear on the plain path.");
        em.flush();
        em.clear();

        FakeLLMService fake = FakeLLMService.returning(devResponse(List.of()));
        new DeveloperAgent(fake, realProps()).runAnalysis(newContext());   // plain entry point

        assertThat(fake.lastUserPrompt)
                .doesNotContain(HEADER)
                .doesNotContain("This must not appear on the plain path.");
    }

    // ---- §19 / §12 headline e2e: create in RUN 1 → retrieve + use in fresh RUN 2
    @Test
    void e2e_memoryCreatedInRun1_isRetrievedAndUsedByAFreshAgentInRun2() {
        // RUN 1 — the Developer intentionally records a durable, shared memory.
        AgentMemoryIntent intent = new AgentMemoryIntent(
                "STARTUP_SHARED", "INSIGHT", "Scanner accuracy is the primary retention driver.", 9);
        FakeLLMService fake1 = FakeLLMService.returning(devResponse(List.of(intent)));
        AgentAnalysisOutcome out1 = new DeveloperAgent(fake1, realProps())
                .runAnalysisWithMemory(newContext(), memory);
        assertThat(out1.failed()).isFalse();

        // Prove it is in the DATABASE (flush + clear the persistence context first).
        em.flush();
        em.clear();
        List<AgentMemory> rows = repository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getContent()).isEqualTo("Scanner accuracy is the primary retention driver.");

        // RUN 2 — a completely fresh service, builder, agent and context (same startup id).
        MemoryService freshService = new MemoryService(repository, mock(EventService.class));
        AgentMemoryContextBuilder freshBuilder =
                new AgentMemoryContextBuilder(freshService, mock(EventService.class));

        // Retrieval occurred and found the persisted row.
        assertThat(freshBuilder.selectRelevant(STARTUP, AgentType.DEVELOPMENT))
                .extracting(MemoryRecord::content)
                .contains("Scanner accuracy is the primary retention driver.");

        FakeLLMService fake2 = FakeLLMService.returning(devResponse(List.of()));
        new DeveloperAgent(fake2, realProps()).runAnalysisWithMemory(newContext(), freshBuilder);

        // The DB-sourced memory is in the fresh agent's reasoning prompt — no manual injection.
        assertThat(fake2.lastUserPrompt)
                .contains(HEADER)
                .contains("Scanner accuracy is the primary retention driver.");
    }
}







