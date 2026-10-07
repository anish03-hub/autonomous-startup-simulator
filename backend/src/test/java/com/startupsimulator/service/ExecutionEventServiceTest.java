package com.startupsimulator.service;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.model.enums.TaskPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionEventServiceTest {

    @Mock
    private EventService eventService;

    private ExecutionEventService executionEventService;

    @BeforeEach
    void setUp() {
        executionEventService = new ExecutionEventService(eventService);
    }

    @Test
    void testEmitExecutionStarted() {
        executionEventService.emitExecutionStarted(1L, 10);
        verify(eventService).record(eq(1L), eq(EventType.EXECUTION_STARTED), anyString(), anyMap());
    }

    @Test
    void testEmitTaskEvents() {
        ExecutionTask task = new ExecutionTask(1L, AgentType.DEVELOPMENT, "Dev Lead", "MVP", "Desc", TaskPriority.HIGH, 5);
        task.setId(100L);

        executionEventService.emitTaskAssigned(1L, task);
        verify(eventService).record(eq(1L), eq(EventType.TASK_ASSIGNED), anyString(), anyMap());

        executionEventService.emitTaskStarted(1L, task);
        verify(eventService).record(eq(1L), eq(EventType.TASK_STARTED), anyString(), anyMap());
        verify(eventService).record(eq(1L), eq(EventType.EMPLOYEE_STARTED_WORK), anyString(), anyMap());

        executionEventService.emitTaskCompleted(1L, task);
        verify(eventService).record(eq(1L), eq(EventType.TASK_COMPLETED), anyString(), anyMap());
        verify(eventService).record(eq(1L), eq(EventType.EMPLOYEE_FINISHED_WORK), anyString(), anyMap());
    }
}
