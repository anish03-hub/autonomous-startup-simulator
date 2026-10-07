package com.startupsimulator.dto.response;

import com.startupsimulator.model.Decision;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.DecisionStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record DecisionDto(
        Long id,
        Long debateId,
        AgentType decidedBy,
        String decision,
        String reason,
        List<String> affectedDepartments,
        DecisionStatus status,
        // ---- Phase 2C: structured CEO synthesis of the boardroom debate ----
        String finalMvpDirection,
        List<String> acceptedArguments,
        List<String> rejectedArguments,
        List<String> finalRisks,
        List<String> finalPriorities,
        Instant createdAt
) {
    public static DecisionDto from(Decision d) {
        if (d == null) {
            return null;
        }
        List<String> affected = d.getAffectedDepartments() == null || d.getAffectedDepartments().isBlank()
                ? List.of()
                : Arrays.stream(d.getAffectedDepartments().split(",")).map(String::trim).toList();
        return new DecisionDto(d.getId(), d.getDebateId(), d.getDecidedBy(), d.getDecision(),
                d.getReason(), affected, d.getStatus(),
                d.getFinalMvpDirection(),
                lines(d.getAcceptedArguments()),
                lines(d.getRejectedArguments()),
                lines(d.getFinalRisks()),
                lines(d.getFinalPriorities()),
                d.getCreatedAt());
    }

    /** Split a newline-joined text column back into a list, empty when blank. */
    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
