package com.startupsimulator.service;

import com.startupsimulator.model.AgentTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskPriority;
import com.startupsimulator.model.enums.TaskStatus;
import com.startupsimulator.repository.AgentTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final AgentTaskRepository taskRepository;

    @Transactional
    public AgentTask create(Long startupId, AgentType agentType, String title, String description,
                            TaskPriority priority, String estimatedCompletion) {
        AgentTask task = new AgentTask(startupId, agentType, title, priority);
        task.setDescription(description);
        task.setEstimatedCompletion(estimatedCompletion);
        task.setStatus(TaskStatus.PENDING);
        return taskRepository.save(task);
    }

    @Transactional
    public AgentTask update(Long taskId, int progress, TaskStatus status) {
        AgentTask task = taskRepository.findById(taskId).orElseThrow();
        task.setProgress(Math.max(0, Math.min(100, progress)));
        task.setStatus(status);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<AgentTask> list(Long startupId) {
        return taskRepository.findByStartupIdOrderById(startupId);
    }
}
