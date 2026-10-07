package com.startupsimulator.service;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockerServiceTest {

    @Mock
    private ExecutionTaskRepository taskRepository;

    private BlockerService blockerService;

    @BeforeEach
    void setUp() {
        blockerService = new BlockerService(taskRepository);
    }

    @Test
    void testCheckForSimulatedBlockerOnDay8() {
        ExecutionTask task = new ExecutionTask(1L, AgentType.DEVELOPMENT, "Dev Lead", "MVP", "Desc", TaskPriority.HIGH, 5);
        task.setId(10L);
        task.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByStartupIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(List.of(task));

        Optional<ExecutionTask> blocked = blockerService.checkForSimulatedBlockers(1L, 8);

        assertTrue(blocked.isPresent());
        assertEquals(TaskStatus.BLOCKED, blocked.get().getStatus());
        assertNotNull(blocked.get().getBlockerReason());
        verify(taskRepository).save(task);
    }

    @Test
    void testResolveBlocker() {
        ExecutionTask task = new ExecutionTask(1L, AgentType.DEVELOPMENT, "Dev Lead", "MVP", "Desc", TaskPriority.HIGH, 5);
        task.setId(10L);
        task.setStatus(TaskStatus.BLOCKED);
        task.setBlockerReason("Risk");

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        ExecutionTask resolved = blockerService.resolveBlocker(10L);

        assertEquals(TaskStatus.IN_PROGRESS, resolved.getStatus());
        assertNull(resolved.getBlockerReason());
        verify(taskRepository).save(task);
    }
}
