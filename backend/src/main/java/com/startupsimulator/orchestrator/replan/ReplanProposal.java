package com.startupsimulator.orchestrator.replan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * CA3 Phase 7: Structured LLM proposal to adapt/replan the persistent startup task graph.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReplanProposal(
        String reason,
        Double confidence,
        Long triggeringTaskId,
        List<ReplanChange> changes,
        String expectedImpact
) {
    public String reasonOrEmpty() {
        return reason == null ? "" : reason.trim();
    }

    public List<ReplanChange> changesOrEmpty() {
        return changes == null ? List.of() : List.copyOf(changes);
    }

    public static ReplanProposal of(String reason, Double confidence, Long triggeringTaskId, List<ReplanChange> changes, String expectedImpact) {
        return new ReplanProposal(reason, confidence, triggeringTaskId, changes, expectedImpact);
    }
}
