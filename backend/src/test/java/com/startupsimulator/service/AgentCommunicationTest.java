package com.startupsimulator.service;

import com.startupsimulator.agent.AgentMessageIntent;
import com.startupsimulator.agent.DeveloperAgent;
import com.startupsimulator.agent.DeveloperAnalysisResponse;
import com.startupsimulator.agent.FakeLLMService;
import com.startupsimulator.agent.FinanceAgent;
import com.startupsimulator.agent.FinanceAnalysisResponse;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.repository.AgentMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CA3 Phase 3 — REAL agent-to-agent communication, end to end.
 *
 * <p>This is the architectural proof demanded by requirement 13: the full chain
 * <em>Development → persist → Finance inbox → Finance prompt → Finance LLM
 * response → Finance→Development reply</em> is exercised against a <b>real</b>
 * {@link AgentMessageRepository} on an embedded H2 database (NOT a Mockito stub
 * and NOT in-memory Java pass-through). The only test doubles are
 * {@link FakeLLMService} (so no network/API is touched) and a mocked
 * {@link EventService} (events are infrastructure, not the message store).
 *
 * <p>Every message an agent "sends" is produced by the LLM as a structured
 * {@link AgentMessageIntent} field (requirement 5 — no tool/function calling),
 * validated and persisted by {@link AgentInbox}, re-queried from the DB, and
 * only then rendered into the next agent's prompt. The {@link DeveloperAgent}
 * never hands a {@link AgentMessage} object to the {@link FinanceAgent}; the
 * message Finance reads carries a DB-assigned id, proving it round-tripped
 * through persistence.
 */
@DataJpaTest
class AgentCommunicationTest {

    private static final long STARTUP_ID = 42L;

    @Autowired
    private AgentMessageRepository repository;

    private EventService eventService;
    private AgentInbox inbox;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        inbox = new AgentInbox(repository, eventService);
        messageService = new MessageService(repository);
    }

    // ---- Fixtures -----------------------------------------------------------

    private static LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    private static StartupContext newContext() {
        Startup s = new Startup();
        s.setId(STARTUP_ID);
        s.setName("GlowRoutine");
        s.setOriginalIdea("An AI-powered app that scans skincare products and recommends a routine.");
        s.setExecutiveSummary("Become the trusted skincare copilot.");
        s.setMvpDirection("Ship a focused scanner-first MVP.");
        return new StartupContext(s);
    }

    /** A valid Development analysis that carries the given outgoing intent (may be null). */
    private static DeveloperAnalysisResponse devResponse(AgentMessageIntent intent) {
        return new DeveloperAnalysisResponse(
                "Modular monolith: React SPA + Spring Boot + PostgreSQL.",
                List.of("React", "TypeScript", "Spring Boot", "PostgreSQL"),
                "Phase 1 (weeks 1-6) core engine; Phase 2 (weeks 7-12) polish.",
                List.of("Model latency depends on data quality."),
                4.0, 80, "An AI scanner-first MVP that personalizes a routine.",
                List.of(new DeveloperAnalysisResponse.FeatureProposal(
                        "Product scanner", "Camera capture feeding the recommendation flow.", true, 3)),
                intent);
    }

    /** A valid Finance analysis that carries the given outgoing intent (may be null). */
    private static FinanceAnalysisResponse finResponse(AgentMessageIntent intent) {
        return new FinanceAnalysisResponse(
                18_000d, 10_000d, 2_000d, 1_600d, 60_000d, 4_200d, 3_000d,
                "Break-even around month 12 at ~1,000 Pro users.",
                72, List.of("Runway is tight at full scope."), intent);
    }

    private DeveloperAgent developer(FakeLLMService llm) {
        return new DeveloperAgent(llm, realProps());
    }

    private FinanceAgent finance(FakeLLMService llm) {
        return new FinanceAgent(llm, realProps());
    }

    /**
     * Mirror {@code StartupOrchestrator.runAnalysisPhase} step 2 exactly for the
     * Development → Finance leg: deliver each department's inbox from the DB, run
     * its analysis, then dispatch whatever it chose to send.
     */
    private void runDevelopmentThenFinance(StartupContext ctx,
                                           DeveloperAgent dev, FinanceAgent fin) {
        inbox.deliverTo(ctx, AgentType.DEVELOPMENT);
        dev.runAnalysis(ctx);
        inbox.dispatchOutgoing(ctx);

        inbox.deliverTo(ctx, AgentType.FINANCE);
        fin.runAnalysis(ctx);
        inbox.dispatchOutgoing(ctx);
    }

    // ---- T1: addressable message is persisted as a real row -----------------

    @Test
    void t1_developmentMessageIsPersistedWithSenderAndTarget() {
        StartupContext ctx = newContext();
        AgentMessageIntent toFinance = new AgentMessageIntent(
                "Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG please confirm the budget for 4 months.");
        runDevelopmentThenFinance(ctx, developer(FakeLLMService.returning(devResponse(toFinance))),
                finance(FakeLLMService.returning(finResponse(null))));

        List<AgentMessage> addressedToFinance =
                repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.FINANCE);
        assertThat(addressedToFinance).hasSize(1);
        AgentMessage row = addressedToFinance.get(0);
        assertThat(row.getId()).isNotNull();                       // DB-assigned — a real row
        assertThat(row.getAgentType()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(row.getTargetAgent()).isEqualTo(AgentType.FINANCE);
        assertThat(row.getContent()).contains("DEV_TO_FIN_MSG");
        assertThat(row.getSubject()).isEqualTo("SCOPE_COST_Q");
    }

    // ---- T2: the recipient's inbox retrieves it and marks it consumed -------

    @Test
    void t2_financeInboxRetrievesAndConsumesTheMessage() {
        StartupContext ctx = newContext();
        AgentMessageIntent toFinance = new AgentMessageIntent(
                "Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG budget check.");
        // Send only (do not deliver yet) so we can observe the unread→consumed flip.
        inbox.send(STARTUP_ID, AgentType.DEVELOPMENT, toFinance);
        assertThat(repository.findByStartupIdAndTargetAgentAndConsumedFalseOrderByIdAsc(
                STARTUP_ID, AgentType.FINANCE)).hasSize(1);

        List<AgentMessage> delivered = inbox.deliverTo(ctx, AgentType.FINANCE);
        assertThat(delivered).hasSize(1);
        assertThat(ctx.inboxFor(AgentType.FINANCE)).hasSize(1);
        // Now consumed — a second delivery retrieves nothing.
        assertThat(repository.findByStartupIdAndTargetAgentAndConsumedFalseOrderByIdAsc(
                STARTUP_ID, AgentType.FINANCE)).isEmpty();
        assertThat(inbox.deliverTo(newContext(), AgentType.FINANCE)).isEmpty();
    }

    // ---- T3 (MOST IMPORTANT): the message appears in Finance's LLM prompt ----

    @Test
    void t3_financePromptContainsTheDevelopmentMessageUnderTheAddressedLabel() {
        StartupContext ctx = newContext();
        AgentMessageIntent toFinance = new AgentMessageIntent(
                "Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG please confirm the budget.");
        FakeLLMService finLlm = FakeLLMService.returning(finResponse(null));
        runDevelopmentThenFinance(ctx, developer(FakeLLMService.returning(devResponse(toFinance))),
                finance(finLlm));

        String financePrompt = finLlm.lastUserPrompt;
        assertThat(financePrompt).contains("=== MESSAGES ADDRESSED TO YOU ===");
        assertThat(financePrompt).contains("These are messages specifically addressed to you");
        assertThat(financePrompt).contains("From Development");
        assertThat(financePrompt).contains("SCOPE_COST_Q");
        assertThat(financePrompt).contains("DEV_TO_FIN_MSG please confirm the budget.");
    }

    // ---- T4: the receiver responds with its own addressable message ----------

    @Test
    void t4_financeRepliesWithAnAddressedMessageToDevelopment() {
        StartupContext ctx = newContext();
        AgentMessageIntent toFinance = new AgentMessageIntent(
                "Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG budget?");
        AgentMessageIntent reply = new AgentMessageIntent(
                "Development", "BUDGET_REPLY", "FIN_TO_DEV_MSG budget confirmed for 4 months.");
        runDevelopmentThenFinance(ctx, developer(FakeLLMService.returning(devResponse(toFinance))),
                finance(FakeLLMService.returning(finResponse(reply))));

        List<AgentMessage> addressedToDevelopment =
                repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.DEVELOPMENT);
        assertThat(addressedToDevelopment).hasSize(1);
        AgentMessage row = addressedToDevelopment.get(0);
        assertThat(row.getAgentType()).isEqualTo(AgentType.FINANCE);
        assertThat(row.getTargetAgent()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(row.getContent()).contains("FIN_TO_DEV_MSG");
    }

    // ---- T5: the recipient is chosen by the LLM intent, not hard-coded -------

    @Test
    void t5_recipientFollowsTheLlmIntentNotAHardCodedTarget() {
        StartupContext ctx = newContext();
        // The model addresses MARKETING, not Finance — the persisted target must follow.
        AgentMessageIntent toMarketing = new AgentMessageIntent(
                "Marketing", "POSITIONING", "DEV_TO_MKT_MSG align the scanner messaging.");
        inbox.deliverTo(ctx, AgentType.DEVELOPMENT);
        developer(FakeLLMService.returning(devResponse(toMarketing))).runAnalysis(ctx);
        inbox.dispatchOutgoing(ctx);

        assertThat(repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.MARKETING))
                .hasSize(1);
        assertThat(repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.FINANCE))
                .isEmpty();
    }

    // ---- T6: an invalid recipient fails explicitly — no row, no redirect -----

    @Test
    void t6_invalidRecipientProducesFailureEventAndNoRow() {
        StartupContext ctx = newContext();
        AgentMessageIntent bogus = new AgentMessageIntent(
                "LegalDept", "N/A", "DEV_TO_NOWHERE this recipient does not exist.");
        inbox.deliverTo(ctx, AgentType.DEVELOPMENT);
        developer(FakeLLMService.returning(devResponse(bogus))).runAnalysis(ctx);
        inbox.dispatchOutgoing(ctx);   // must not throw

        assertThat(repository.count()).isZero();                    // no invalid row created
        verify(eventService).record(eq(STARTUP_ID), eq(EventType.AGENT_MESSAGE_FAILED), any(), any());
        // and it was NOT silently redirected to any real department
        for (AgentType t : AgentType.values()) {
            assertThat(repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, t)).isEmpty();
        }
    }

    // ---- T7: ordering is preserved (chronological, by id) --------------------

    @Test
    void t7_messagesToTheSameRecipientPreserveOrder() {
        inbox.send(STARTUP_ID, AgentType.DEVELOPMENT,
                new AgentMessageIntent("Finance", "Q1", "first"));
        inbox.send(STARTUP_ID, AgentType.MARKETING,
                new AgentMessageIntent("Finance", "Q2", "second"));
        inbox.send(STARTUP_ID, AgentType.CEO,
                new AgentMessageIntent("Finance", "Q3", "third"));

        List<AgentMessage> ordered = messageService.addressedTo(STARTUP_ID, AgentType.FINANCE);
        assertThat(ordered).extracting(AgentMessage::getContent)
                .containsExactly("first", "second", "third");
    }

    // ---- T8: REAL mode exercises the structured messageIntent path -----------

    @Test
    void t8_realModeDrivesMessagingFromTheStructuredLlmOutput() {
        StartupContext ctx = newContext();
        FakeLLMService devLlm = FakeLLMService.returning(devResponse(
                new AgentMessageIntent("Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG real path.")));
        FakeLLMService finLlm = FakeLLMService.returning(finResponse(null));
        runDevelopmentThenFinance(ctx, developer(devLlm), finance(finLlm));

        assertThat(devLlm.structuredCalls).isEqualTo(1);            // real LLM branch taken
        assertThat(finLlm.structuredCalls).isEqualTo(1);
        assertThat(repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.FINANCE))
                .hasSize(1);                                        // message came from model output
    }

    // ---- T9: SCRIPTED_DEMO makes no LLM call and sends no messages -----------

    @Test
    void t9_scriptedDemoRunsDeterministicallyWithoutMessaging() {
        StartupContext ctx = newContext();
        FakeLLMService devLlm = FakeLLMService.returning(devResponse(
                new AgentMessageIntent("Finance", "X", "should never be used in scripted mode")));
        DeveloperAgent scriptedDev = new DeveloperAgent(devLlm, new LlmProperties()); // SCRIPTED_DEMO

        inbox.deliverTo(ctx, AgentType.DEVELOPMENT);
        scriptedDev.runAnalysis(ctx);
        inbox.dispatchOutgoing(ctx);

        assertThat(devLlm.structuredCalls).isZero();                // deterministic path, no API
        assertThat(repository.count()).isZero();                    // no Phase-3 messages queued
    }

    // ---- T10: the message store is real persistence, not UI fabrication ------

    @Test
    void t10_messageIsAuthoritativePersistenceNotUiOnly() {
        StartupContext ctx = newContext();
        AgentMessageIntent toFinance = new AgentMessageIntent(
                "Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG persisted independently of any event.");
        runDevelopmentThenFinance(ctx, developer(FakeLLMService.returning(devResponse(toFinance))),
                finance(FakeLLMService.returning(finResponse(null))));

        // Re-query from the repository (not from an event/DTO): the content round-trips through the DB.
        AgentMessage persisted =
                repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.FINANCE).get(0);
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getContent()).isEqualTo("DEV_TO_FIN_MSG persisted independently of any event.");
        assertThat(persisted.isConsumed()).isTrue();                // delivered to Finance's inbox
    }

    // ---- T13 (ARCHITECTURAL): the full chain, link by link -------------------

    @Test
    void t13_fullChainDevelopmentPersistFinanceInboxPromptResponse() {
        StartupContext ctx = newContext();
        AgentMessageIntent devToFin = new AgentMessageIntent(
                "Finance", "SCOPE_COST_Q", "DEV_TO_FIN_MSG can we afford 4 months?");
        AgentMessageIntent finToDev = new AgentMessageIntent(
                "Development", "BUDGET_REPLY", "FIN_TO_DEV_MSG yes, within runway.");
        FakeLLMService devLlm = FakeLLMService.returning(devResponse(devToFin));
        FakeLLMService finLlm = FakeLLMService.returning(finResponse(finToDev));

        runDevelopmentThenFinance(ctx, developer(devLlm), finance(finLlm));

        // Link 1: Development's structured output persisted a real DEV→FIN row.
        List<AgentMessage> toFinance =
                repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.FINANCE);
        assertThat(toFinance).hasSize(1);
        AgentMessage dbRow = toFinance.get(0);
        assertThat(dbRow.getId()).isNotNull();
        assertThat(dbRow.getAgentType()).isEqualTo(AgentType.DEVELOPMENT);

        // Link 2: it was delivered to Finance's inbox and the object Finance saw is the
        //         re-queried DB row (same id) — NOT an in-memory hand-off from Development.
        List<AgentMessage> financeInbox = ctx.inboxFor(AgentType.FINANCE);
        assertThat(financeInbox).hasSize(1);
        assertThat(financeInbox.get(0).getId()).isEqualTo(dbRow.getId());
        assertThat(dbRow.isConsumed()).isTrue();

        // Link 3: Finance's LLM prompt actually contained that message.
        assertThat(finLlm.lastUserPrompt)
                .contains("These are messages specifically addressed to you")
                .contains("DEV_TO_FIN_MSG can we afford 4 months?");

        // Link 4: Finance's own structured output persisted a real FIN→DEV reply row.
        List<AgentMessage> toDevelopment =
                repository.findByStartupIdAndTargetAgentOrderByIdAsc(STARTUP_ID, AgentType.DEVELOPMENT);
        assertThat(toDevelopment).hasSize(1);
        assertThat(toDevelopment.get(0).getAgentType()).isEqualTo(AgentType.FINANCE);
        assertThat(toDevelopment.get(0).getContent()).contains("FIN_TO_DEV_MSG");

        // Bounded: exactly two messages crossed the wire — no unrestricted autonomous loop.
        assertThat(repository.count()).isEqualTo(2);
        verify(eventService, never())
                .record(eq(STARTUP_ID), eq(EventType.AGENT_MESSAGE_FAILED), any(), any());
    }
}
