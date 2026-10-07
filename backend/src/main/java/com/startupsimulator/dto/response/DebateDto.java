package com.startupsimulator.dto.response;

import com.startupsimulator.model.Debate;
import com.startupsimulator.model.enums.DebateStatus;

import java.time.Instant;
import java.util.List;

public record DebateDto(
        Long id,
        String topic,
        String question,
        DebateStatus status,
        Instant startedAt,
        Instant resolvedAt,
        List<AgentMessageDto> messages,
        DecisionDto decision
) {
    public static DebateDto from(Debate d, List<AgentMessageDto> messages, DecisionDto decision) {
        return new DebateDto(d.getId(), d.getTopic(), d.getQuestion(), d.getStatus(),
                d.getStartedAt(), d.getResolvedAt(), messages, decision);
    }
}
