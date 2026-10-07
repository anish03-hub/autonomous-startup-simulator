package com.startupsimulator.dto.response;

import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.enums.AgentType;

import java.time.Instant;

public record AgentMessageDto(
        Long id,
        AgentType agentType,
        String agentLabel,
        Long debateId,
        Integer debateRound,
        String messageType,
        AgentType targetAgent,
        String subject,
        String content,
        Instant createdAt
) {
    public static AgentMessageDto from(AgentMessage m) {
        return new AgentMessageDto(
                m.getId(), m.getAgentType(), m.getAgentType().getDisplayName(),
                m.getDebateId(), m.getDebateRound(), m.getMessageType(), m.getTargetAgent(),
                m.getSubject(), m.getContent(), m.getCreatedAt());
    }
}
