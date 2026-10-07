package com.startupsimulator.service;

import com.startupsimulator.dto.response.BlueprintDto;
import com.startupsimulator.dto.response.MvpFeatureDetailDto;
import com.startupsimulator.dto.response.MvpSectionDto;
import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.repository.ExecutionTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskGeneratorTest {

    @Mock
    private ExecutionTaskRepository taskRepository;

    private TaskGenerator taskGenerator;

    @BeforeEach
    void setUp() {
        taskGenerator = new TaskGenerator(taskRepository);
        when(taskRepository.save(any(ExecutionTask.class))).thenAnswer(invocation -> {
            ExecutionTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                // Assign dummy incremental ID
                task.setId((long) (Math.random() * 1000 + 1));
            }
            return task;
        });
    }

    @Test
    void testGenerateTasksFromBlueprint() {
        MvpFeatureDetailDto feature1 = new MvpFeatureDetailDto(1L, "AI Content Engine", "Automated marketing post generator", true, "v1.0", 3, "HIGH", "Core value prop", "DEVELOPMENT");
        MvpSectionDto mvpSection = new MvpSectionDto(List.of(feature1), List.of(), "CEO Synthesis");
        BlueprintDto blueprint = BlueprintDto.builder()
                .startupId(42L)
                .name("Nova AI")
                .pitch("AI Growth Platform")
                .mvpSection(mvpSection)
                .build();

        List<ExecutionTask> tasks = taskGenerator.generateTasks(42L, blueprint);

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        verify(taskRepository, atLeastOnce()).deleteByStartupId(42L);

        // Verify tasks span all 4 departments
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == AgentType.CEO));
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == AgentType.DEVELOPMENT));
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == AgentType.MARKETING));
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == AgentType.FINANCE));

        // Verify dynamic MVP task creation
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().contains("AI Content Engine")));
    }
}
