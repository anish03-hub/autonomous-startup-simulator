package com.startupsimulator.dto.response;

import com.startupsimulator.model.AgentTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskPriority;
import com.startupsimulator.model.enums.TaskStatus;

public record AgentTaskDto(
        Long id,
        AgentType agentType,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        int progress,
        String estimatedCompletion
) {
    public static AgentTaskDto from(AgentTask t) {
        return new AgentTaskDto(
                t.getId(), t.getAgentType(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getPriority(), t.getProgress(), t.getEstimatedCompletion());
    }
}
