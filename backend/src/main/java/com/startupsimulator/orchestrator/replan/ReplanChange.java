package com.startupsimulator.orchestrator.replan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

/**
 * CA3 Phase 7: Represents a single proposed modification to the persistent task plan.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReplanChange(
        String actionType,
        Long targetTaskId,
        String title,
        String description,
        String department,
        String priority,
        Long dependencyId,
        List<Long> dependencies,
        String reason
) {
    public Optional<ReplanActionType> resolvedActionType() {
        return ReplanActionType.resolve(actionType);
    }

    public static ReplanChange addTask(String department, String title, String description, String priority, List<Long> dependencies, String reason) {
        return new ReplanChange(ReplanActionType.ADD_TASK.name(), null, title, description, department, priority, null, dependencies, reason);
    }

    public static ReplanChange modifyTask(Long targetTaskId, String title, String description, String priority, String reason) {
        return new ReplanChange(ReplanActionType.MODIFY_TASK.name(), targetTaskId, title, description, null, priority, null, null, reason);
    }

    public static ReplanChange deferTask(Long targetTaskId, String reason) {
        return new ReplanChange(ReplanActionType.DEFER_TASK.name(), targetTaskId, null, null, null, null, null, null, reason);
    }

    public static ReplanChange cancelTask(Long targetTaskId, String reason) {
        return new ReplanChange(ReplanActionType.CANCEL_TASK.name(), targetTaskId, null, null, null, null, null, null, reason);
    }

    public static ReplanChange changeOwner(Long targetTaskId, String newDepartment, String reason) {
        return new ReplanChange(ReplanActionType.CHANGE_OWNER.name(), targetTaskId, null, null, newDepartment, null, null, null, reason);
    }

    public static ReplanChange addDependency(Long targetTaskId, Long dependencyId, String reason) {
        return new ReplanChange(ReplanActionType.ADD_DEPENDENCY.name(), targetTaskId, null, null, null, null, dependencyId, null, reason);
    }

    public static ReplanChange removeDependency(Long targetTaskId, Long dependencyId, String reason) {
        return new ReplanChange(ReplanActionType.REMOVE_DEPENDENCY.name(), targetTaskId, null, null, null, null, dependencyId, null, reason);
    }
}
