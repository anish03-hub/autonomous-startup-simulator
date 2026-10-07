package com.startupsimulator.agent;

import com.startupsimulator.model.enums.AgentType;

import java.util.List;

/** Result of the CEO resolving a boardroom debate. */
public record DecisionOutcome(
        String decision,
        String reason,
        List<AgentType> affectedDepartments
) {
}
