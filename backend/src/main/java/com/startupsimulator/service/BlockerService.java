package com.startupsimulator.service;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.TaskStatus;
import com.startupsimulator.repository.ExecutionTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages execution risk events, blockers, and resolution flows.
 */
@Service
@RequiredArgsConstructor
public class BlockerService {

    private final ExecutionTaskRepository taskRepository;

    @Transactional
    public Optional<ExecutionTask> checkForSimulatedBlockers(Long startupId, int currentDay) {
        List<ExecutionTask> inProgress = taskRepository.findByStartupIdAndStatus(startupId, TaskStatus.IN_PROGRESS);

        if (inProgress.isEmpty()) {
            return Optional.empty();
        }

        // Deterministic risk trigger condition (e.g. Day 8 or Day 18) to simulate realistic project risks
        if (currentDay == 8 || currentDay == 18) {
            ExecutionTask target = inProgress.get(0);
            target.setStatus(TaskStatus.BLOCKED);
            String reason = target.getDepartment() + " execution blocked on Day " + currentDay +
                    ": Dependencies delayed due to technical risk review.";
            target.setBlockerReason(reason);
            taskRepository.save(target);
            return Optional.of(target);
        }

        return Optional.empty();
    }

    @Transactional
    public ExecutionTask resolveBlocker(Long taskId) {
        ExecutionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.BLOCKED) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setBlockerReason(null);
            taskRepository.save(task);
        }
        return task;
    }
}
