package com.startupsimulator.orchestrator.replan;

import java.util.Optional;

/**
 * CA3 Phase 7: Typed replanning action types that an LLM can propose to mutate
 * the persistent task plan when execution encounters a blocker or new requirements.
 */
public enum ReplanActionType {
    ADD_TASK,
    MODIFY_TASK,
    DEFER_TASK,
    CANCEL_TASK,
    CHANGE_OWNER,
    ADD_DEPENDENCY,
    REMOVE_DEPENDENCY;

    public static Optional<ReplanActionType> resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String s = raw.trim();
        for (ReplanActionType a : values()) {
            if (a.name().equalsIgnoreCase(s)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }
}
