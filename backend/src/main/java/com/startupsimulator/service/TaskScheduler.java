package com.startupsimulator.service;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.TaskStatus;
import com.startupsimulator.repository.ExecutionTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TaskScheduler respects task dependencies (DAG) and assigns unblocked tasks
 * to available department agents.
 */
@Service
@RequiredArgsConstructor
public class TaskScheduler {

    private final ExecutionTaskRepository taskRepository;

    @Transactional
    public List<ExecutionTask> scheduleAvailableTasks(Long startupId) {
        List<ExecutionTask> allTasks = taskRepository.findByStartupIdOrderById(startupId);

        Set<Long> completedTaskIds = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .map(ExecutionTask::getId)
                .collect(Collectors.toSet());

        // Find departments currently working on an IN_PROGRESS task
        Set<String> busyDepartments = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.BLOCKED)
                .map(t -> t.getDepartment().name())
                .collect(Collectors.toSet());

        List<ExecutionTask> newlyScheduled = new ArrayList<>();

        for (ExecutionTask task : allTasks) {
            if (task.getStatus() == TaskStatus.PENDING) {
                // Check if all dependencies are satisfied
                List<Long> deps = task.getDependencies();
                boolean dependenciesSatisfied = deps.isEmpty() || completedTaskIds.containsAll(deps);

                // Assign if dependencies satisfied and department is not currently occupied
                if (dependenciesSatisfied && !busyDepartments.contains(task.getDepartment().name())) {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    if (task.getProgress() <= 0.0) {
                        task.setProgress(1.0);
                    }
                    taskRepository.save(task);
                    newlyScheduled.add(task);
                    busyDepartments.add(task.getDepartment().name());
                }
            }
        }

        return newlyScheduled;
    }
}
