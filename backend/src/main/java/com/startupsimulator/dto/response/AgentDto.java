package com.startupsimulator.dto.response;

import com.startupsimulator.model.Agent;
import com.startupsimulator.model.enums.AgentState;
import com.startupsimulator.model.enums.AgentType;

public record AgentDto(
        Long id,
        AgentType type,
        String typeLabel,
        String name,
        AgentState state,
        String currentActivity,
        String mandate
) {
    public static AgentDto from(Agent a) {
        return new AgentDto(
                a.getId(), a.getType(), a.getType().getDisplayName(), a.getName(),
                a.getState(), a.getCurrentActivity(), a.getType().getMandate());
    }
}
