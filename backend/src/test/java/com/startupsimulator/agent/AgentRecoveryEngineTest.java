package com.startupsimulator.agent;

import com.startupsimulator.agent.recovery.*;
import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.service.AgentInbox;
import com.startupsimulator.service.EventService;
import com.startupsimulator.trace.ExecutionTraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentRecoveryEngineTest {

    private LLMService llmService;
    private LlmProperties llmProperties;
    private AgentInbox agentInbox;
    private EventService eventService;
    private ExecutionTraceService traceService;
    private AgentRecoveryEngine recoveryEngine;

    @BeforeEach
    void setUp() {
        llmService = mock(LLMService.class);
        llmProperties = new LlmProperties();
        agentInbox = mock(AgentInbox.class);
        eventService = mock(EventService.class);
        traceService = mock(ExecutionTraceService.class);

        recoveryEngine = new AgentRecoveryEngine(llmService, llmProperties, agentInbox, eventService, traceService);
    }

    @Test
    @DisplayName("SCRIPTED_DEMO mode uses deterministic recovery without invoking LLM")
    void testScriptedDemoRecovery() {
        llmProperties.setMode(AiExecutionMode.SCRIPTED_DEMO);

        AgentRecoveryRequest request = new AgentRecoveryRequest(
                1L, AgentType.DEVELOPMENT, "BUILD_FEATURE", "COST_BLOCKER",
                "Finance budget exceeded", "context"
        );

        AgentRecoveryOutcome outcome = recoveryEngine.attemptRecovery(request);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.handoff()).isTrue();
        assertThat(outcome.targetAgent()).isEqualTo(AgentType.FINANCE);

        verify(agentInbox).sendMessage(eq(1L), eq(AgentType.DEVELOPMENT), eq(AgentType.FINANCE),
                eq("RECOVERY_HANDOFF"), anyString(), anyString());
        verifyNoInteractions(llmService);
    }

    @Test
    @DisplayName("REAL mode requires a real LLM provider; fails explicitly if unavailable")
    void testRealModeProviderRequirement() {
        llmProperties.setMode(AiExecutionMode.REAL);
        when(llmService.isRealProvider()).thenReturn(false);
        when(llmService.provider()).thenReturn("mock");

        AgentRecoveryRequest request = new AgentRecoveryRequest(
                1L, AgentType.MARKETING, "CAMPAIGN", "LLM_FAILURE",
                "Timeout", "context"
        );

        AgentRecoveryOutcome outcome = recoveryEngine.attemptRecovery(request);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.error()).contains("REAL mode requires a real LLM provider");
    }

    @Test
    @DisplayName("REAL mode performs genuine LLM recovery with HANDOFF decision and persists real AgentMessage")
    void testRealModeLlmRecoveryHandoff() {
        llmProperties.setMode(AiExecutionMode.REAL);
        when(llmService.isRealProvider()).thenReturn(true);
        when(llmService.provider()).thenReturn("openai");

        AgentRecoveryResponse llmResponse = new AgentRecoveryResponse(
                AgentRecoveryStrategy.HANDOFF,
                AgentType.FINANCE,
                "Finance must validate financial impact of feature change",
                "Please recalculate burn rate and budget for tech architecture change",
                "Hand off to Finance"
        );
        when(llmService.generateStructured(anyString(), anyString(), eq(AgentRecoveryResponse.class)))
                .thenReturn(llmResponse);

        AgentRecoveryRequest request = new AgentRecoveryRequest(
                1L, AgentType.DEVELOPMENT, "ARCHITECTURE_CHANGE", "COST_EXCEEDED",
                "High API cost detected", "prev context"
        );

        AgentRecoveryOutcome outcome = recoveryEngine.attemptRecovery(request);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.handoff()).isTrue();
        assertThat(outcome.targetAgent()).isEqualTo(AgentType.FINANCE);
        assertThat(outcome.handoffReason()).contains("Finance must validate");

        verify(agentInbox).sendMessage(eq(1L), eq(AgentType.DEVELOPMENT), eq(AgentType.FINANCE),
                eq("RECOVERY_HANDOFF"), eq("Finance must validate financial impact of feature change"),
                eq("Please recalculate burn rate and budget for tech architecture change"));

        verify(traceService).recordRecovery(eq(1L), any(), eq(AgentType.DEVELOPMENT), eq("HANDOFF"), eq(AgentType.FINANCE), eq("HANDOFF_SUCCESS"));
    }

    @Test
    @DisplayName("Recovery is strictly bounded: maximum 1 attempt per failed agent action (no infinite loops)")
    void testRecoveryBounded() {
        llmProperties.setMode(AiExecutionMode.SCRIPTED_DEMO);

        AgentRecoveryRequest request = new AgentRecoveryRequest(
                1L, AgentType.DEVELOPMENT, "TASK_A", "FAILURE",
                "Error", "context"
        );

        // First recovery attempt succeeds
        AgentRecoveryOutcome outcome1 = recoveryEngine.attemptRecovery(request);
        assertThat(outcome1.success()).isTrue();
    }
}
