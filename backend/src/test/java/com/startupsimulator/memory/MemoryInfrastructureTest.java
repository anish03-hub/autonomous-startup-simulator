package com.startupsimulator.memory;

import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CA3 Phase 5A — persistent agent-memory infrastructure, proven against a real
 * embedded H2 database (NOT a Mockito stub, NOT an in-memory Java pass-through),
 * exactly like the Phase 3 {@code AgentCommunicationTest}. The only test doubles
 * are a mocked {@link EventService} (events are infrastructure, not the store).
 *
 * <p>The headline proof (T3 / §22): a {@link CreateMemoryRequest} saved through
 * one {@link MemoryService} instance is retrieved — after the persistence context
 * is flushed and cleared — through a <b>different</b> service instance, proving the
 * memory survived beyond the original service/object lifetime.
 */
@DataJpaTest
class MemoryInfrastructureTest {

    private static final long STARTUP_A = 100L;
    private static final long STARTUP_B = 200L;

    @Autowired
    private AgentMemoryRepository repository;

    @Autowired
    private TestEntityManager em;

    private EventService eventService;
    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        memoryService = new MemoryService(repository, eventService);
    }

    // ---- T1: creation persists a real row -----------------------------------

    @Test
    void t1_createPersistsMemoryRowWithDbAssignedId() {
        MemoryRecord saved = memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.DECISION,
                "MVP should prioritize inventory tracking before analytics."));

        assertThat(saved.id()).isNotNull();                          // DB-assigned — a real row
        AgentMemory row = repository.findById(saved.id()).orElseThrow();
        assertThat(row.getStartupId()).isEqualTo(STARTUP_A);
        assertThat(row.getAgentType()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(row.getScope()).isEqualTo(MemoryScope.AGENT_PRIVATE);
        assertThat(row.getMemoryType()).isEqualTo(MemoryType.DECISION);
        assertThat(row.getContent()).isEqualTo("MVP should prioritize inventory tracking before analytics.");
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isNotNull();
    }

    // ---- T2: retrieval by agent ----------------------------------------------

    @Test
    void t2_retrievalByAgentReturnsThatAgentsPrivateMemory() {
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.FINANCE, MemoryType.FACT,
                "Infrastructure budget is capped at $2,000/month."));

        List<MemoryRecord> finance = memoryService.findForAgent(STARTUP_A, AgentType.FINANCE);
        assertThat(finance).hasSize(1);
        assertThat(finance.get(0).content()).contains("$2,000/month");
        assertThat(finance.get(0).agentType()).isEqualTo(AgentType.FINANCE);
    }

    // ---- T3 (MOST IMPORTANT / §22): survives beyond the original service -----

    @Test
    void t3_memorySurvivesBeyondOriginalServiceAndContext() {
        // Run 1: save through the ORIGINAL service instance.
        MemoryRecord saved = memoryService.save(CreateMemoryRequest.sharedMemory(
                STARTUP_A, AgentType.CEO, MemoryType.INSIGHT,
                "Scanner accuracy is the single biggest driver of retention."));
        Long id = saved.id();
        assertThat(id).isNotNull();

        // Evict the whole persistence context so nothing can be served from the
        // first-level cache — the next read MUST come back from the database.
        em.flush();
        em.clear();

        // Run 2: a brand-new service + the repository re-read the row from the DB.
        MemoryService freshService = new MemoryService(repository, mock(EventService.class));
        List<MemoryRecord> shared = freshService.findShared(STARTUP_A);

        assertThat(shared).hasSize(1);
        assertThat(shared.get(0).id()).isEqualTo(id);                 // same persisted row
        assertThat(shared.get(0).content())
                .isEqualTo("Scanner accuracy is the single biggest driver of retention.");
    }

    // ---- T4: startup isolation -----------------------------------------------

    @Test
    void t4_startupAMemoryDoesNotAppearForStartupB() {
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.MARKETING, MemoryType.PREFERENCE, "Position as premium, not cheapest."));

        assertThat(memoryService.findForAgent(STARTUP_A, AgentType.MARKETING)).hasSize(1);
        assertThat(memoryService.findForAgent(STARTUP_B, AgentType.MARKETING)).isEmpty();
        assertThat(memoryService.findRecent(STARTUP_B, 10)).isEmpty();
    }

    // ---- T5: agent isolation (Finance not in Development retrieval) ----------

    @Test
    void t5_oneAgentsPrivateMemoryNeverLeaksIntoAnothersRetrieval() {
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.FINANCE, MemoryType.RISK, "Runway is tight at full scope."));
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.DECISION, "Use a modular monolith."));

        List<MemoryRecord> dev = memoryService.findForAgent(STARTUP_A, AgentType.DEVELOPMENT);
        assertThat(dev).hasSize(1);
        assertThat(dev).noneMatch(r -> r.agentType() == AgentType.FINANCE);
        assertThat(dev.get(0).content()).isEqualTo("Use a modular monolith.");
    }

    // ---- T6: shared memory retrieved by scope, one canonical row -------------

    @Test
    void t6_sharedMemoryIsOneRowRetrievedByScopeNotCopiedPerAgent() {
        memoryService.save(CreateMemoryRequest.sharedMemory(
                STARTUP_A, AgentType.CEO, MemoryType.FACT, "The company pivoted to a scanner-first MVP."));

        // Exactly one row exists for the shared memory (not copied to four agents).
        assertThat(repository.count()).isEqualTo(1);
        assertThat(memoryService.find(MemoryQuery.shared(STARTUP_A))).hasSize(1);

        // It does NOT bleed into any agent's private retrieval.
        for (AgentType t : AgentType.values()) {
            assertThat(memoryService.findForAgent(STARTUP_A, t)).isEmpty();
        }
    }

    // ---- T7: memory-type filtering -------------------------------------------

    @Test
    void t7_memoryTypeFilteringReturnsOnlyThatType() {
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.DECISION, "Adopt PostgreSQL."));
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.RISK, "Model latency depends on data quality."));

        List<MemoryRecord> risks = memoryService.find(MemoryQuery.byType(STARTUP_A, MemoryType.RISK));
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0).memoryType()).isEqualTo(MemoryType.RISK);
    }

    // ---- T8: recent ordering is deterministic (newest first) ----------------

    @Test
    void t8_recentOrderingIsDeterministicNewestFirst() {
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.FACT, "first"));
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.MARKETING, MemoryType.FACT, "second"));
        memoryService.save(CreateMemoryRequest.sharedMemory(
                STARTUP_A, AgentType.CEO, MemoryType.FACT, "third"));

        // id-tiebreak makes this stable even when createdAt Instants collide.
        assertThat(memoryService.findRecent(STARTUP_A, 10))
                .extracting(MemoryRecord::content)
                .containsExactly("third", "second", "first");
    }

    // ---- T9: invalid (blank) content is rejected -----------------------------

    @Test
    void t9_blankContentIsRejectedAndNoRowIsWritten() {
        assertThatThrownBy(() -> memoryService.save(new CreateMemoryRequest(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryScope.AGENT_PRIVATE,
                MemoryType.FACT, "   ", 5, null, null)))
                .isInstanceOf(MemoryValidationException.class);
        assertThat(repository.count()).isZero();
        verify(eventService, never()).record(any(), eq(EventType.MEMORY_CREATED), any(), any());
    }

    // ---- T10: invalid (null) agent is rejected safely ------------------------

    @Test
    void t10_nullAgentIsRejectedSafelyWithNoRow() {
        assertThatThrownBy(() -> memoryService.save(new CreateMemoryRequest(
                STARTUP_A, null, MemoryScope.AGENT_PRIVATE,
                MemoryType.FACT, "orphan memory", 5, null, null)))
                .isInstanceOf(MemoryValidationException.class);
        assertThat(repository.count()).isZero();
    }

    // ---- T11: exact duplicate does not create a second row -------------------

    @Test
    void t11_exactDuplicateDoesNotCreateDuplicateRows() {
        CreateMemoryRequest req = CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.DECISION, "Ship the scanner-first MVP.");
        MemoryRecord first = memoryService.save(req);
        MemoryRecord second = memoryService.save(req);   // identical candidate

        assertThat(repository.count()).isEqualTo(1);                 // still one row
        assertThat(second.id()).isEqualTo(first.id());               // the pre-existing row

        // A different memoryType for the same content is NOT a duplicate.
        memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.FACT, "Ship the scanner-first MVP."));
        assertThat(repository.count()).isEqualTo(2);
    }

    // ---- T12: MEMORY_CREATED only after an actual persistence ----------------

    @Test
    void t12_memoryCreatedEventOnlyFiresAfterRealPersistence() {
        CreateMemoryRequest req = CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.FINANCE, MemoryType.LESSON, "Validate pricing before scaling spend.");
        memoryService.save(req);
        verify(eventService).record(eq(STARTUP_A), eq(EventType.MEMORY_CREATED), any(), any());

        // The duplicate save persists nothing new, so it emits no further event.
        memoryService.save(req);
        verify(eventService).record(eq(STARTUP_A), eq(EventType.MEMORY_CREATED), any(), any());
        assertThat(repository.count()).isEqualTo(1);
    }

    // ---- T13: the service is the boundary — DTOs out, no JPA type leaks ------

    @Test
    void t13_serviceBoundaryExposesDtosNeverJpaEntitiesOrRepository() {
        for (Method m : MemoryService.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            assertThat(m.getReturnType()).isNotIn(AgentMemory.class, AgentMemoryRepository.class);
            for (Class<?> p : m.getParameterTypes()) {
                assertThat(p).isNotIn(AgentMemory.class, AgentMemoryRepository.class);
            }
        }
        // save takes the typed request and returns the read-model DTO.
        assertThat(memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.CEO, MemoryType.FACT, "boundary check")))
                .isInstanceOf(MemoryRecord.class);
    }

    // ---- T14: absurdly large content is rejected -----------------------------

    @Test
    void t14_absurdlyLargeContentIsRejected() {
        String huge = "x".repeat(MemoryService.MAX_CONTENT_LENGTH + 1);
        assertThatThrownBy(() -> memoryService.save(CreateMemoryRequest.privateMemory(
                STARTUP_A, AgentType.DEVELOPMENT, MemoryType.FACT, huge)))
                .isInstanceOf(MemoryValidationException.class);
        assertThat(repository.count()).isZero();
    }

    // ---- T15: closed vocabularies (guards the schema contract) ---------------

    @Test
    void t15_memoryVocabulariesAreTheClosedPhase5aSets() {
        // A small, intentional taxonomy — not dozens, not open-ended.
        assertThat(MemoryType.values())
                .containsExactly(MemoryType.DECISION, MemoryType.FACT, MemoryType.INSIGHT,
                        MemoryType.LESSON, MemoryType.RISK, MemoryType.PREFERENCE);
        assertThat(MemoryScope.values())
                .containsExactly(MemoryScope.AGENT_PRIVATE, MemoryScope.STARTUP_SHARED);
    }

    // ---- Extra: invalid retrieval is a controlled failure, not a throw -------

    @Test
    void invalidRetrievalReturnsEmptyAndEmitsFailureEvent() {
        List<MemoryRecord> result = memoryService.findForAgent(STARTUP_A, null);
        assertThat(result).isEmpty();                                 // controlled, no throw
        verify(eventService).record(eq(STARTUP_A), eq(EventType.MEMORY_RETRIEVAL_FAILED), any(), any());
    }

    @Test
    void invalidImportanceIsRejected() {
        assertThatThrownBy(() -> memoryService.save(new CreateMemoryRequest(
                STARTUP_A, AgentType.CEO, MemoryScope.AGENT_PRIVATE,
                MemoryType.FACT, "bad importance", 99, null, null)))
                .isInstanceOf(MemoryValidationException.class);
        assertThat(repository.count()).isZero();
    }
}
