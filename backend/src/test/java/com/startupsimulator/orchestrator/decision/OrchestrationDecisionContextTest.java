package com.startupsimulator.orchestrator.decision;

import com.startupsimulator.agent.AgentMemoryContextBuilder;
import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.memory.AgentMemoryRepository;
import com.startupsimulator.memory.CreateMemoryRequest;
import com.startupsimulator.memory.MemoryScope;
import com.startupsimulator.memory.MemoryService;
import com.startupsimulator.memory.MemoryType;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.SimulationPhase;
import com.startupsimulator.repository.AgentMessageRepository;
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
 * CA3 Phase 6A — the {@code DataJpaTest}-backed proofs that the orchestration
 * decision prompt is actually fed by the REAL stores: persistent memory flows in
 * through the existing Phase 5B {@link AgentMemoryContextBuilder}/{@link MemoryService}
 * (§21) and inter-department messages flow in through the real
 * {@link AgentMessageRepository} (§22) — not an in-memory Java shortcut. The
 * {@link OrchestrationStateSnapshotFactory} assembles the snapshot from those
 * stores, the engine renders it, and a controlled {@link FakeLLMService} proves
 * the model both SEES that context and may act on it. Finance is never hard-coded
 * in Java: the engine only validates and accepts what the model proposed.
 *
 * <p>The pure-unit decision proofs (dynamic selection, invalid/illegal rejection,
 * bounded history, REAL failure, SCRIPTED_DEMO) live in
 * {@link OrchestrationDecisionEngineTest}.
 */
@DataJpaTest
class OrchestrationDecisionContextTest {

    private static final long STARTUP = 1L;

    @Autowired
    private AgentMemoryRepository memoryRepository;

    @Autowired
    private AgentMessageRepository messageRepository;

    @Autowired
    private TestEntityManager em;

    private EventService events;
    private MemoryService memoryService;
    private AgentMemoryContextBuilder memoryContextBuilder;
    private OrchestrationStateSnapshotFactory snapshotFactory;
    private OrchestrationDecisionValidator validator;

    @BeforeEach
    void setUp() {
        events = mock(EventService.class);
        memoryService = new MemoryService(memoryRepository, events);
        memoryContextBuilder = new AgentMemoryContextBuilder(memoryService, events);
        snapshotFactory = new OrchestrationStateSnapshotFactory(memoryContextBuilder, messageRepository);
        validator = new OrchestrationDecisionValidator();
    }

    private OrchestrationDecisionEngine engine(FakeLLMService llm) {
        return new OrchestrationDecisionEngine(llm, realProps(), validator, events);
    }

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

    // ---- §21: a persistent memory actually enters the orchestration prompt, and
    //           the model's rationale may cite it. Key proof: the memory is PASSED
    //           to the LLM, not merely stored. --------------------------------------
    @Test
    void t21_persistentMemory_entersThePrompt_andRationaleCanUseIt() {
        String regulatory = "The target customer requires regulatory approval before deployment.";
        memoryService.save(new CreateMemoryRequest(
                STARTUP, AgentType.CEO, MemoryScope.STARTUP_SHARED, MemoryType.RISK,
                regulatory, 9, "seed", null));
        em.flush();
        em.clear();

        OrchestrationStateSnapshot snapshot = snapshotFactory.build(
                newContext(), SimulationPhase.ANALYSIS,
                List.of(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING),
                List.of(AgentType.FINANCE),
                List.of(), false, false, "NOT_STARTED", List.of());

        // The model reads the regulatory memory and chooses Finance, citing it.
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "Finance must model the cost of regulatory approval.",
                "Persistent memory notes the customer requires regulatory approval before "
                        + "deployment, so Finance should quantify its cost next."));

        OrchestrationDecisionOutcome out = engine(fake).decideNext(snapshot);

        // The memory was genuinely injected into the orchestration LLM prompt.
        assertThat(fake.lastUserPrompt)
                .contains("=== RELEVANT PERSISTENT MEMORY ===")
                .contains(regulatory);
        // ...and the accepted decision is the model's, with a rationale that used it.
        assertThat(out.isAccepted()).isTrue();
        assertThat(out.resolvedAgent()).isEqualTo(AgentType.FINANCE);
        assertThat(out.decision().rationaleOrEmpty()).contains("regulatory approval");
    }

    // ---- §22: a real Developer→Finance message enters the prompt, the model sees
    //           it and selects Finance. Finance is NOT hard-coded in Java. ----------
    @Test
    void t22_recentMessage_entersThePrompt_andInformsTheDecision() {
        AgentMessage devToFinance = new AgentMessage(STARTUP,
                AgentType.DEVELOPMENT, AgentType.FINANCE,
                "Cost validation",
                "Implementation cost assumptions need financial validation.");
        messageRepository.save(devToFinance);
        em.flush();
        em.clear();

        OrchestrationStateSnapshot snapshot = snapshotFactory.build(
                newContext(), SimulationPhase.ANALYSIS,
                List.of(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING),
                List.of(AgentType.FINANCE),
                List.of(), false, false, "NOT_STARTED", List.of());

        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent(
                "FINANCE", "Development asked Finance to validate the cost assumptions.",
                "A recent Development → Finance message requests financial validation of "
                        + "implementation cost assumptions."));

        OrchestrationDecisionOutcome out = engine(fake).decideNext(snapshot);

        // The real inter-department message was surfaced into the prompt.
        assertThat(fake.lastUserPrompt)
                .contains("=== RECENT AGENT MESSAGES ===")
                .contains("Implementation cost assumptions need financial validation.")
                .contains(AgentType.DEVELOPMENT.getDisplayName())
                .contains(AgentType.FINANCE.getDisplayName());
        // The accepted agent came from the LLM response, validated by Java.
        assertThat(out.isAccepted()).isTrue();
        assertThat(out.resolvedAgent()).isEqualTo(AgentType.FINANCE);
    }

    // ---- Memory and messages are kept DISTINCT inputs, never collapsed ----------
    @Test
    void tMemoryAndMessages_areSeparateSections_notMerged() {
        memoryService.save(new CreateMemoryRequest(
                STARTUP, AgentType.CEO, MemoryScope.STARTUP_SHARED, MemoryType.FACT,
                "Pricing must stay under 50 USD per month.", 7, "seed", null));
        messageRepository.save(new AgentMessage(STARTUP,
                AgentType.MARKETING, AgentType.FINANCE, "Pricing", "Confirm the price point."));
        em.flush();
        em.clear();

        OrchestrationStateSnapshot snapshot = snapshotFactory.build(
                newContext(), SimulationPhase.ANALYSIS,
                List.of(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING),
                List.of(AgentType.FINANCE),
                List.of(), false, false, "NOT_STARTED", List.of());
        FakeLLMService fake = FakeLLMService.returning(OrchestrationDecision.runAgent("FINANCE", "x", "x"));

        engine(fake).decideNext(snapshot);

        String prompt = fake.lastUserPrompt;
        int messagesHeader = prompt.indexOf("=== RECENT AGENT MESSAGES ===");
        int memoryHeader = prompt.indexOf("=== RELEVANT PERSISTENT MEMORY ===");
        assertThat(messagesHeader).isGreaterThanOrEqualTo(0);
        assertThat(memoryHeader).isGreaterThan(messagesHeader);   // two distinct, ordered sections
    }
}
