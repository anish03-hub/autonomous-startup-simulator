package com.startupsimulator.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupsimulator.dto.response.ExecutionTraceEntryDto;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.repository.AgentMessageRepository;
import com.startupsimulator.service.AgentInbox;
import com.startupsimulator.service.EventService;
import com.startupsimulator.trace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class SectionAMultiAgentIntegrationTest {

    @Autowired
    private ExecutionTraceRepository traceRepository;

    @Autowired
    private AgentMessageRepository messageRepository;

    private ExecutionTraceService traceService;
    private AgentInbox agentInbox;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        traceService = new ExecutionTraceService(traceRepository, new ObjectMapper());
        agentInbox = new AgentInbox(messageRepository, eventService, traceService);
    }

    @Test
    @DisplayName("CA3 Section A Target: 4 Agents (CEO, Dev, Mkt, Fin), Reasoning, Tools, Memory, Communication, Recovery & Queryable Execution Trace")
    void testCompleteSectionAMultiAgentTraceScenario() {
        Long startupId = 999L;
        String traceId = "run-section-a-test";

        Map<String, Object> toolArgs = Map.of("snippet", "function calculateLatency() { return 42; }");

        // 1. CEO Strategic Reasoning Action
        traceService.recordAgentAction(startupId, traceId, AgentType.CEO, "STRATEGIC_ANALYSIS",
                "CEO analyzed market opportunity and set mandate for Development and Marketing.", "openai");

        // 2. Dev Agent Tool Selection & Execution
        traceService.recordToolCall(startupId, traceId, AgentType.DEVELOPMENT, "code_evaluator", toolArgs, "Dev testing backend service latency");
        traceService.recordToolResult(startupId, traceId, AgentType.DEVELOPMENT, "code_evaluator", true, "Execution successful. Output: 42ms");

        // 3. Dev Agent discovers blocker and sends addressable message to Finance
        agentInbox.sendMessage(startupId, AgentType.DEVELOPMENT, AgentType.FINANCE,
                "RECOVERY_HANDOFF", "High cloud architecture cost", "Please evaluate AWS cloud budget for new AI microservice.");

        // 4. Finance Agent retrieves unread message from inbox & memory
        traceService.recordMemoryRetrieval(startupId, traceId, AgentType.FINANCE, "STARTUP_SHARED", 2);

        Startup startup = new Startup("SaaS Platform", "AI Analytics");
        startup.setId(startupId);
        StartupContext ctx = new StartupContext(startup);

        List<AgentMessage> financeInbox = agentInbox.deliverTo(ctx, AgentType.FINANCE);
        assertThat(financeInbox).isNotEmpty();
        assertThat(financeInbox.get(0).getAgentType()).isEqualTo(AgentType.DEVELOPMENT);

        // 5. Orchestration Decision & Replan Triggered
        traceService.recordOrchestrationDecision(startupId, traceId, "REPLAN", AgentType.CEO, "Cost blocker identified, initiating replan", true);
        traceService.recordReplan(startupId, traceId, "Dev Cloud Task", "TASK_MUTATION", "Insert Finance Cost Validation Task", true);

        // 6. Failure Recovery Event
        traceService.recordRecovery(startupId, traceId, AgentType.DEVELOPMENT, "HANDOFF", AgentType.FINANCE, "HANDOFF_SUCCESS");

        // 7. Verify Queryable Execution Trace via Endpoint Service
        List<ExecutionTraceEntryDto> traceEntries = traceService.getTrace(startupId);

        assertThat(traceEntries).isNotEmpty();
        assertThat(traceEntries).extracting(ExecutionTraceEntryDto::type)
                .contains(
                        ExecutionTraceType.AGENT_ACTION,
                        ExecutionTraceType.TOOL_CALL,
                        ExecutionTraceType.TOOL_RESULT,
                        ExecutionTraceType.AGENT_MESSAGE,
                        ExecutionTraceType.MEMORY_RETRIEVAL,
                        ExecutionTraceType.ORCHESTRATION_DECISION,
                        ExecutionTraceType.REPLAN,
                        ExecutionTraceType.RECOVERY
                );

        // Verify sequence ordering is strictly ascending
        for (int i = 0; i < traceEntries.size() - 1; i++) {
            assertThat(traceEntries.get(i).sequenceNumber()).isLessThan(traceEntries.get(i + 1).sequenceNumber());
        }
    }
}
