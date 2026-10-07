package com.startupsimulator.orchestrator.replan;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CA3 Phase 7: Authoritative server-side validation boundary for LLM-generated {@link ReplanProposal}s.
 * Enforces schema correctness, task existence, lifecycle legality, DAG cycle prohibition, and safety limits.
 */
@Component
public class ReplanValidator {

    public static final int MAX_CHANGES_PER_REPLAN = 5;
    public static final int MAX_TOTAL_TASKS_PER_STARTUP = 25;

    public ReplanValidationOutcome validate(ReplanProposal proposal, List<ExecutionTask> existingTasks) {
        if (proposal == null) {
            return ReplanValidationOutcome.rejected("Proposal is null.", null);
        }

        List<ReplanChange> changes = proposal.changesOrEmpty();
        if (changes.isEmpty()) {
            return ReplanValidationOutcome.rejected("Proposal contains no changes.", proposal);
        }

        if (changes.size() > MAX_CHANGES_PER_REPLAN) {
            return ReplanValidationOutcome.rejected("Proposal exceeds maximum allowed changes per replan ("
                    + MAX_CHANGES_PER_REPLAN + "). Provided: " + changes.size(), proposal);
        }

        Map<Long, ExecutionTask> taskMap = new HashMap<>();
        if (existingTasks != null) {
            for (ExecutionTask t : existingTasks) {
                taskMap.put(t.getId(), t);
            }
        }

        long addCount = changes.stream().filter(c -> c.resolvedActionType().filter(a -> a == ReplanActionType.ADD_TASK).isPresent()).count();
        if (taskMap.size() + addCount > MAX_TOTAL_TASKS_PER_STARTUP) {
            return ReplanValidationOutcome.rejected("Applying proposal would exceed maximum allowed tasks per startup ("
                    + MAX_TOTAL_TASKS_PER_STARTUP + "). Current: " + taskMap.size() + ", Adding: " + addCount, proposal);
        }

        // Build adjacency map for DAG cycle detection: task -> set of dependency task IDs
        Map<Long, Set<Long>> adj = new HashMap<>();
        for (ExecutionTask t : existingTasks) {
            adj.put(t.getId(), new HashSet<>(t.getDependencies()));
        }

        // Simulated task IDs for ADD_TASK entries in this proposal (using negative IDs for validation tracking)
        long tempIdCounter = -1;
        Map<Integer, Long> changeIdxToTempId = new HashMap<>();

        for (int i = 0; i < changes.size(); i++) {
            ReplanChange change = changes.get(i);
            Optional<ReplanActionType> actionOpt = change.resolvedActionType();
            if (actionOpt.isEmpty()) {
                return ReplanValidationOutcome.rejected("Change #" + (i + 1) + " has unrecognised actionType '"
                        + change.actionType() + "'.", proposal);
            }
            ReplanActionType action = actionOpt.get();

            switch (action) {
                case ADD_TASK -> {
                    if (change.title() == null || change.title().isBlank()) {
                        return ReplanValidationOutcome.rejected("ADD_TASK requires a non-blank title.", proposal);
                    }
                    if (change.department() == null || resolveAgentType(change.department()).isEmpty()) {
                        return ReplanValidationOutcome.rejected("ADD_TASK requires a valid department (AgentType), got: '"
                                + change.department() + "'.", proposal);
                    }
                    Long newTempId = tempIdCounter--;
                    changeIdxToTempId.put(i, newTempId);
                    Set<Long> deps = new HashSet<>();
                    if (change.dependencies() != null) {
                        for (Long depId : change.dependencies()) {
                            if (!taskMap.containsKey(depId) && !changeIdxToTempId.containsValue(depId)) {
                                return ReplanValidationOutcome.rejected("ADD_TASK references non-existent dependencyId: "
                                        + depId, proposal);
                            }
                            deps.add(depId);
                        }
                    }
                    adj.put(newTempId, deps);
                }
                case MODIFY_TASK -> {
                    Long tid = change.targetTaskId();
                    if (tid == null || !taskMap.containsKey(tid)) {
                        return ReplanValidationOutcome.rejected("MODIFY_TASK targetTaskId " + tid + " does not exist.", proposal);
                    }
                    if (taskMap.get(tid).getStatus() == TaskStatus.COMPLETED) {
                        return ReplanValidationOutcome.rejected("Cannot MODIFY_TASK already COMPLETED task #" + tid + ".", proposal);
                    }
                }
                case DEFER_TASK -> {
                    Long tid = change.targetTaskId();
                    if (tid == null || !taskMap.containsKey(tid)) {
                        return ReplanValidationOutcome.rejected("DEFER_TASK targetTaskId " + tid + " does not exist.", proposal);
                    }
                    if (taskMap.get(tid).getStatus() == TaskStatus.COMPLETED) {
                        return ReplanValidationOutcome.rejected("Cannot DEFER_TASK already COMPLETED task #" + tid + ".", proposal);
                    }
                }
                case CANCEL_TASK -> {
                    Long tid = change.targetTaskId();
                    if (tid == null || !taskMap.containsKey(tid)) {
                        return ReplanValidationOutcome.rejected("CANCEL_TASK targetTaskId " + tid + " does not exist.", proposal);
                    }
                    if (taskMap.get(tid).getStatus() == TaskStatus.COMPLETED) {
                        return ReplanValidationOutcome.rejected("Cannot CANCEL_TASK already COMPLETED task #" + tid + ".", proposal);
                    }
                }
                case CHANGE_OWNER -> {
                    Long tid = change.targetTaskId();
                    if (tid == null || !taskMap.containsKey(tid)) {
                        return ReplanValidationOutcome.rejected("CHANGE_OWNER targetTaskId " + tid + " does not exist.", proposal);
                    }
                    if (taskMap.get(tid).getStatus() == TaskStatus.COMPLETED) {
                        return ReplanValidationOutcome.rejected("Cannot CHANGE_OWNER on COMPLETED task #" + tid + ".", proposal);
                    }
                    if (change.department() == null || resolveAgentType(change.department()).isEmpty()) {
                        return ReplanValidationOutcome.rejected("CHANGE_OWNER requires a valid department, got: '"
                                + change.department() + "'.", proposal);
                    }
                }
                case ADD_DEPENDENCY -> {
                    Long tid = change.targetTaskId();
                    Long depId = change.dependencyId();
                    if (tid == null || !taskMap.containsKey(tid)) {
                        return ReplanValidationOutcome.rejected("ADD_DEPENDENCY targetTaskId " + tid + " does not exist.", proposal);
                    }
                    if (depId == null || (!taskMap.containsKey(depId) && !changeIdxToTempId.containsValue(depId))) {
                        return ReplanValidationOutcome.rejected("ADD_DEPENDENCY dependencyId " + depId + " does not exist.", proposal);
                    }
                    if (tid.equals(depId)) {
                        return ReplanValidationOutcome.rejected("Cannot ADD_DEPENDENCY to self (task #" + tid + ").", proposal);
                    }
                    if (taskMap.get(tid).getStatus() == TaskStatus.COMPLETED) {
                        return ReplanValidationOutcome.rejected("Cannot ADD_DEPENDENCY to COMPLETED task #" + tid + ".", proposal);
                    }
                    adj.computeIfAbsent(tid, k -> new HashSet<>()).add(depId);
                }
                case REMOVE_DEPENDENCY -> {
                    Long tid = change.targetTaskId();
                    Long depId = change.dependencyId();
                    if (tid == null || !taskMap.containsKey(tid)) {
                        return ReplanValidationOutcome.rejected("REMOVE_DEPENDENCY targetTaskId " + tid + " does not exist.", proposal);
                    }
                    if (depId == null) {
                        return ReplanValidationOutcome.rejected("REMOVE_DEPENDENCY requires a dependencyId.", proposal);
                    }
                    if (adj.containsKey(tid)) {
                        adj.get(tid).remove(depId);
                    }
                }
            }
        }

        // Validate DAG (no dependency cycles)
        if (hasCycle(adj)) {
            return ReplanValidationOutcome.rejected("Applying proposal would introduce a dependency cycle into the task graph.", proposal);
        }

        return ReplanValidationOutcome.accepted(proposal);
    }

    private static Optional<AgentType> resolveAgentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (AgentType a : AgentType.values()) {
            if (a.name().equalsIgnoreCase(raw.trim())) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /** Simple DFS cycle detection in task dependency graph. */
    private boolean hasCycle(Map<Long, Set<Long>> adj) {
        Set<Long> visited = new HashSet<>();
        Set<Long> recStack = new HashSet<>();

        for (Long node : adj.keySet()) {
            if (detectCycleDFS(node, adj, visited, recStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCycleDFS(Long node, Map<Long, Set<Long>> adj, Set<Long> visited, Set<Long> recStack) {
        if (recStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recStack.add(node);

        Set<Long> neighbors = adj.getOrDefault(node, Collections.emptySet());
        for (Long neighbor : neighbors) {
            if (detectCycleDFS(neighbor, adj, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }
}
