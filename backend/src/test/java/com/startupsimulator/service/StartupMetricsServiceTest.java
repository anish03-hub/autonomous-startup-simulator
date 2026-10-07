package com.startupsimulator.service;

import com.startupsimulator.model.Budget;
import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskPriority;
import com.startupsimulator.model.enums.TaskStatus;
import com.startupsimulator.repository.ExecutionTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartupMetricsServiceTest {

    @Mock
    private ExecutionTaskRepository taskRepository;

    @Mock
    private StartupContextService contextService;

    private StartupMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new StartupMetricsService(taskRepository, contextService);
    }

    @Test
    void testCalculateMetrics() {
        ExecutionTask task1 = new ExecutionTask(10L, AgentType.DEVELOPMENT, "Dev Lead", "Architecture", "Desc", TaskPriority.HIGH, 4);
        task1.setProgress(100.0);
        task1.setStatus(TaskStatus.COMPLETED);

        ExecutionTask task2 = new ExecutionTask(10L, AgentType.MARKETING, "Growth Lead", "Campaign", "Desc", TaskPriority.HIGH, 5);
        task2.setProgress(50.0);
        task2.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByStartupIdOrderById(10L)).thenReturn(List.of(task1, task2));

        Budget mockBudget = new Budget();
        mockBudget.setStartingCapital(150000.0);
        mockBudget.setMonthlyBurn(15000.0);
        when(contextService.budget(10L)).thenReturn(mockBudget);

        Map<String, Object> metrics = metricsService.calculateMetrics(10L, 5); // Day 5

        assertNotNull(metrics);
        assertEquals(75.0, (double) metrics.get("overallProgress"));
        assertEquals(100.0, (double) metrics.get("mvpProgress"));
        assertEquals(50.0, (double) metrics.get("marketReadiness"));
        assertTrue((double) metrics.get("budgetRemaining") < 150000.0);
        assertTrue((double) metrics.get("runwayMonths") > 0.0);
    }
}
