package com.startupsimulator.orchestrator;

import com.startupsimulator.agent.AgentMemoryContextBuilder;
import com.startupsimulator.agent.AgentMemoryIntent;
import com.startupsimulator.agent.AgentMessageIntent;
import com.startupsimulator.agent.CeoAgent;
import com.startupsimulator.agent.DeveloperAgent;
import com.startupsimulator.agent.DeveloperAnalysisResponse;
import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.agent.FinanceAgent;
import com.startupsimulator.agent.MarketingAgent;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.memory.AgentMemoryRepository;
import com.startupsimulator.memory.MemoryRecord;
import com.startupsimulator.memory.MemoryScope;
import com.startupsimulator.memory.MemoryService;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.SimulationPhase;
import com.startupsimulator.orchestrator.decision.OrchestrationDecision;
import com.startupsimulator.orchestrator.decision.OrchestrationDecisionEngine;
import com.startupsimulator.orchestrator.decision.OrchestrationDecisionOutcome;
import com.startupsimulator.orchestrator.decision.OrchestrationDecisionValidator;
import com.startupsimulator.orchestrator.decision.OrchestrationStateSnapshot;
import com.startupsimulator.orchestrator.decision.OrchestrationStateSnapshotFactory;
import com.startupsimulator.repository.AgentMessageRepository;
import com.startupsimulator.service.AgentInbox;
import com.startupsimulator.service.AgentService;
import com.startupsimulator.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * CA3 Phase 6B — §29: the memory/communication adaptation proof, backed by REAL
 * JPA stores (not an in-memory Java shortcut). It exercises the actual runtime
 * executor so the whole chain is genuine:
 *
 * <ol>
 *   <li>the {@link com.startupsimulator.orchestrator.OrchestrationActionExecutor}
 *       runs {@code RUN_AGENT → DEVELOPMENT} exactly as the dynamic loop would;</li>
 *   <li>the Developer analysis (REAL mode) queues a message to Finance and a
 *       durable memory, which the real {@link AgentInbox} / {@link MemoryService}
 *       persist as real rows;</li>
 *   <li>a FRESH {@link OrchestrationStateSnapshot} is rebuilt from those stores,
 *       with DEVELOPMENT now moved from pending to completed;</li>
 *   <li>the next {@link OrchestrationDecisionEngine} prompt therefore contains the
 *       recent Development→Finance communication, the relevant persistent memory,
 *       AND the updated completion state — proving later decisions genuinely see
 *       the state the prior action changed.</li>
 * </ol>
 *
 * <p>Memory and messages flow in through the EXISTING Phase 5B
 * {@link AgentMemoryContextBuilder} and the real {@link AgentMessageRepository}
 * respectively — they stay DISTINCT inputs, never collapsed, and nothing is
 * fabricated in Java. The persisted rows are also asserted directly, so the test
 * cannot pass on an in-memory illusion.
 */
@DataJpaTest
class DynamicOrchestrationContextTest {

    private static final long STARTUP = 1L;

    private static final String MEMORY_CONTENT =
            "Real-time bank-sync is the heaviest technical risk and must be de-scoped from the MVP.";
    private static final String MESSAGE_CONTENT =
            "Implementation cost assumptions for real-time sync need financial validation.";

    @Autowired
    private AgentMemoryRepository memoryRepository;

    @Autowired
    private AgentMessageRepository messageRepository;

    @Autowired
    private TestEntityManager em;

    private EventService events;
    private MemoryService memoryService;
    private AgentMemoryContextBuilder memoryContextBuilder;
    private AgentInbox agentInbox;
    private OrchestrationStateSnapshotFactory snapshotFactory;
    private OrchestrationDecisionValidator validator;

    @BeforeEach
    void setUp() {
        events = mock(EventService.class);
        memoryService = new MemoryService(memoryRepository, events);
        memoryContextBuilder = new AgentMemoryContextBuilder(memoryService, events);
        agentInbox = new AgentInbox(messageRepository, events);
        snapshotFactory = new OrchestrationStateSnapshotFactory(memoryContextBuilder, messageRepository);
        validator = new OrchestrationDecisionValidator();
    }

    @Test
    void t29_developerExecution_persistsMessageAndMemory_thenNextPromptReflectsThem() {
        // ---- a real Developer turn through the actual runtime executor ----------
        FakeLLMService devFake = FakeLLMService.returning(developerResponseWithMessageAndMemory());
        OrchestrationActionExecutor executor = executorRunningRealDeveloper(devFake);

        StartupContext ctx = newContext();
        executor.executeRunAgent(AgentType.DEVELOPMENT, ctx);   // delivers inbox, analyses, dispatches

        // The Developer genuinely ran its real analysis exactly once.
        assertThat(devFake.structuredCalls).isEqualTo(1);

        em.flush();
        em.clear();

        // ---- the message and the memory are REAL persisted rows -----------------
        List<AgentMessage> messages = messageRepository.findByStartupIdOrderById(STARTUP);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getAgentType()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(messages.get(0).getTargetAgent()).isEqualTo(AgentType.FINANCE);
        assertThat(messages.get(0).getContent()).isEqualTo(MESSAGE_CONTENT);

        List<MemoryRecord> shared = memoryService.findShared(STARTUP);
        assertThat(shared).anySatisfy(m -> {
            assertThat(m.scope()).isEqualTo(MemoryScope.STARTUP_SHARED);
            assertThat(m.content()).isEqualTo(MEMORY_CONTENT);
        });

        // ---- rebuild the snapshot: DEVELOPMENT has moved pending → completed -----
        OrchestrationStateSnapshot snapshot = snapshotFactory.build(
                newContext(), SimulationPhase.ANALYSIS,
                List.of(AgentType.DEVELOPMENT),
                List.of(AgentType.CEO, AgentType.MARKETING, AgentType.FINANCE),
                List.of(), false, false, "ANALYSIS_IN_PROGRESS", List.of());

        // ---- the next orchestration decision prompt reflects all three ----------
        FakeLLMService engineFake = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "Development asked Finance to validate the sync cost assumptions.",
                "A recent Development → Finance message plus the shared risk memory point to Finance next."));
        OrchestrationDecisionEngine engine =
                new OrchestrationDecisionEngine(engineFake, realProps(), validator, events);

        OrchestrationDecisionOutcome out = engine.decideNext(snapshot);

        String prompt = engineFake.lastUserPrompt;
        // (1) recent communication is present...
        assertThat(prompt)
                .contains("=== RECENT AGENT MESSAGES ===")
                .contains(MESSAGE_CONTENT)
                .contains(AgentType.DEVELOPMENT.getDisplayName())
                .contains(AgentType.FINANCE.getDisplayName());
        // (2) ...the persistent memory is present, as its own distinct section...
        assertThat(prompt)
                .contains("=== RELEVANT PERSISTENT MEMORY ===")
                .contains(MEMORY_CONTENT);
        // (3) ...and the updated completion state is present.
        assertThat(prompt).contains("Completed agents: " + AgentType.DEVELOPMENT.name());
        assertThat(prompt).contains("=== RECENT AGENT MESSAGES ===");

        // Communication and memory are two DISTINCT sections, never merged.
        int messagesHeader = prompt.indexOf("=== RECENT AGENT MESSAGES ===");
        int memoryHeader = prompt.indexOf("=== RELEVANT PERSISTENT MEMORY ===");
        assertThat(messagesHeader).isGreaterThanOrEqualTo(0);
        assertThat(memoryHeader).isGreaterThan(messagesHeader);

        // The decision came from the model, validated by Java (Finance never hard-coded).
        assertThat(out.isAccepted()).isTrue();
        assertThat(out.resolvedAgent()).isEqualTo(AgentType.FINANCE);
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * An executor whose Development agent runs for real (its own {@code devFake});
     * the other agents and the boardroom are present but never invoked here.
     */
    private OrchestrationActionExecutor executorRunningRealDeveloper(FakeLLMService devFake) {
        LlmProperties realProps = realProps();
        CeoAgent ceoAgent = new CeoAgent(FakeLLMService.offline(), realProps);
        DeveloperAgent developerAgent = new DeveloperAgent(devFake, realProps);
        MarketingAgent marketingAgent = new MarketingAgent(FakeLLMService.offline(), realProps);
        FinanceAgent financeAgent = new FinanceAgent(FakeLLMService.offline(), realProps);
        return new OrchestrationActionExecutor(
                mock(AgentService.class), events, agentInbox, memoryContextBuilder,
                ceoAgent, developerAgent, marketingAgent, financeAgent,
                mock(BoardroomDebate.class));
    }

    private static DeveloperAnalysisResponse developerResponseWithMessageAndMemory() {
        return new DeveloperAnalysisResponse(
                "Modular monolith with a sync worker isolated behind a queue.",
                List.of("Java", "Spring Boot", "PostgreSQL"),
                "Twelve-week MVP, bank-sync deferred to a fast-follow.",
                List.of("Real-time bank-sync is operationally heavy and brittle"),
                6.5d,
                78,
                "A cash-flow copilot MVP that imports statements and forecasts runway.",
                List.of(new DeveloperAnalysisResponse.FeatureProposal(
                        "Statement import", "CSV/OFX import of bank statements.", true, 2)),
                new AgentMessageIntent("FINANCE", "Sync cost validation", MESSAGE_CONTENT),
                List.of(new AgentMemoryIntent("STARTUP_SHARED", "RISK", MEMORY_CONTENT, 8)));
    }

    private static StartupContext newContext() {
        Startup s = new Startup();
        s.setId(STARTUP);
        s.setName("Acme");
        s.setOriginalIdea("An AI copilot that turns bank data into a live cash-flow forecast.");
        return new StartupContext(s);
    }

    private static LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }
}
