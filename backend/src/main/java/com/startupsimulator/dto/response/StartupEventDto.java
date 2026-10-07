package com.startupsimulator.dto.response;

import com.startupsimulator.model.StartupEvent;
import com.startupsimulator.model.enums.EventType;

import java.time.Instant;

public record StartupEventDto(
        Long id,
        Long startupId,
        EventType type,
        String message,
        String payload,
        Instant createdAt
) {
    public static StartupEventDto from(StartupEvent e) {
        return new StartupEventDto(e.getId(), e.getStartupId(), e.getType(),
                e.getMessage(), e.getPayload(), e.getCreatedAt());
    }
}
