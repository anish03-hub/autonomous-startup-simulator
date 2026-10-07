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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskSchedulerTest {

    @Mock
    private ExecutionTaskRepository taskRepository;

    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        taskScheduler = new TaskScheduler(taskRepository);
    }

    @Test
    void testScheduleAvailableTasksWithSatisfiedDependencies() {
        ExecutionTask task1 = new ExecutionTask(100L, AgentType.CEO, "CEO", "Kickoff", "Desc", TaskPriority.HIGH, 2);
        task1.setId(1L);
        task1.setStatus(TaskStatus.COMPLETED);

        ExecutionTask task2 = new ExecutionTask(100L, AgentType.DEVELOPMENT, "Dev Lead", "Dev Task", "Desc", TaskPriority.HIGH, 5);
        task2.setId(2L);
        task2.setDependencies(List.of(1L));
        task2.setStatus(TaskStatus.PENDING);

        when(taskRepository.findByStartupIdOrderById(100L)).thenReturn(List.of(task1, task2));

        List<ExecutionTask> scheduled = taskScheduler.scheduleAvailableTasks(100L);

        assertEquals(1, scheduled.size());
        assertEquals(2L, scheduled.get(0).getId());
        assertEquals(TaskStatus.IN_PROGRESS, scheduled.get(0).getStatus());
        verify(taskRepository).save(task2);
    }

    @Test
    void testBlockedTaskIsNotScheduledIfDependenciesUnsatisfied() {
        ExecutionTask task1 = new ExecutionTask(100L, AgentType.CEO, "CEO", "Kickoff", "Desc", TaskPriority.HIGH, 2);
        task1.setId(1L);
        task1.setStatus(TaskStatus.PENDING); // Not completed yet

        ExecutionTask task2 = new ExecutionTask(100L, AgentType.DEVELOPMENT, "Dev Lead", "Dev Task", "Desc", TaskPriority.HIGH, 5);
        task2.setId(2L);
        task2.setDependencies(List.of(1L));
        task2.setStatus(TaskStatus.PENDING);

        when(taskRepository.findByStartupIdOrderById(100L)).thenReturn(List.of(task1, task2));

        List<ExecutionTask> scheduled = taskScheduler.scheduleAvailableTasks(100L);

        // Only task1 (no deps) should be scheduled
        assertEquals(1, scheduled.size());
        assertEquals(1L, scheduled.get(0).getId());
    }
}
