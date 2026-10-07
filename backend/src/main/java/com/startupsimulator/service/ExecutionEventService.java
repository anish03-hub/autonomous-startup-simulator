package com.startupsimulator.service;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * High-level helper service for recording execution SSE events.
 */
@Service
@RequiredArgsConstructor
public class ExecutionEventService {

    private final EventService eventService;

    public void emitExecutionStarted(Long startupId, int totalTasks) {
        eventService.record(
                startupId,
                EventType.EXECUTION_STARTED,
                "🚀 Autonomous Startup Execution started with " + totalTasks + " executable tasks.",
                Map.of("totalTasks", totalTasks)
        );
    }

    public void emitTaskAssigned(Long startupId, ExecutionTask task) {
        eventService.record(
                startupId,
                EventType.TASK_ASSIGNED,
                "📋 Task assigned to " + task.getAssignedAgent() + ": " + task.getTitle(),
                Map.of("taskId", task.getId(), "department", task.getDepartment(), "agent", task.getAssignedAgent(), "title", task.getTitle())
        );
    }

    public void emitTaskStarted(Long startupId, ExecutionTask task) {
        eventService.record(
                startupId,
                EventType.TASK_STARTED,
                "⚡ Task started: " + task.getTitle(),
                Map.of("taskId", task.getId(), "department", task.getDepartment(), "agent", task.getAssignedAgent())
        );
        eventService.record(
                startupId,
                EventType.EMPLOYEE_STARTED_WORK,
                "👨‍💻 " + task.getAssignedAgent() + " began working on " + task.getTitle(),
                Map.of("department", task.getDepartment(), "agent", task.getAssignedAgent(), "taskId", task.getId())
        );
    }

    public void emitTaskProgress(Long startupId, ExecutionTask task) {
        eventService.record(
                startupId,
                EventType.TASK_PROGRESS,
                "📈 " + task.getAssignedAgent() + " made progress on " + task.getTitle() + " (" + (int) task.getProgress() + "%)",
                Map.of("taskId", task.getId(), "department", task.getDepartment(), "progress", task.getProgress())
        );
    }

    public void emitTaskCompleted(Long startupId, ExecutionTask task) {
        eventService.record(
                startupId,
                EventType.TASK_COMPLETED,
                "✅ Task completed by " + task.getAssignedAgent() + ": " + task.getTitle(),
                Map.of("taskId", task.getId(), "department", task.getDepartment(), "title", task.getTitle())
        );
        eventService.record(
                startupId,
                EventType.EMPLOYEE_FINISHED_WORK,
                "🎉 " + task.getAssignedAgent() + " finished work on " + task.getTitle(),
                Map.of("department", task.getDepartment(), "agent", task.getAssignedAgent(), "taskId", task.getId())
        );
    }

    public void emitTaskBlocked(Long startupId, ExecutionTask task) {
        eventService.record(
                startupId,
                EventType.TASK_BLOCKED,
                "⚠️ Task blocked: " + task.getTitle() + " - " + task.getBlockerReason(),
                Map.of("taskId", task.getId(), "department", task.getDepartment(), "reason", task.getBlockerReason())
        );
        eventService.record(
                startupId,
                EventType.CEO_INTERVENTION_REQUIRED,
                "🚨 CEO Intervention Required: " + task.getDepartment() + " task is blocked.",
                Map.of("taskId", task.getId(), "department", task.getDepartment())
        );
    }

    public void emitMetricChanged(Long startupId, Map<String, Object> metrics) {
        eventService.record(
                startupId,
                EventType.METRIC_CHANGED,
                "📊 Startup metrics updated: " + metrics.get("overallProgress") + "% overall progress.",
                metrics
        );
    }

    public void emitExecutionPaused(Long startupId) {
        eventService.record(
                startupId,
                EventType.EXECUTION_PAUSED,
                "⏸️ Startup execution paused.",
                Map.of("status", "PAUSED")
        );
    }

    public void emitExecutionResumed(Long startupId) {
        eventService.record(
                startupId,
                EventType.EXECUTION_RESUMED,
                "▶️ Startup execution resumed.",
                Map.of("status", "RESUMED")
        );
    }

    public void emitExecutionCompleted(Long startupId, double overallProgress) {
        eventService.record(
                startupId,
                EventType.EXECUTION_COMPLETED,
                "🏆 Startup Execution Completed! All roadmap tasks successfully delivered (" + overallProgress + "%).",
                Map.of("overallProgress", overallProgress)
        );
    }
}
