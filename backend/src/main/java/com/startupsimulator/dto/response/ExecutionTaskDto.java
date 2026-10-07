package com.startupsimulator.dto.response;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskPriority;
import com.startupsimulator.model.enums.TaskStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record ExecutionTaskDto(
        Long id,
        Long startupId,
        AgentType department,
        String assignedAgent,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        double progress,
        List<Long> dependencies,
        int estimatedDays,
        int actualDays,
        String blockerReason
) {
    public static ExecutionTaskDto from(ExecutionTask task) {
        if (task == null) return null;
        return ExecutionTaskDto.builder()
                .id(task.getId())
                .startupId(task.getStartupId())
                .department(task.getDepartment())
                .assignedAgent(task.getAssignedAgent())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .progress(task.getProgress())
                .dependencies(task.getDependencies())
                .estimatedDays(task.getEstimatedDays())
                .actualDays(task.getActualDays())
                .blockerReason(task.getBlockerReason())
                .build();
    }
}
