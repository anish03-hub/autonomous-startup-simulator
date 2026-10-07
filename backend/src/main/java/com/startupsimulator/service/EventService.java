package com.startupsimulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupsimulator.dto.response.StartupEventDto;
import com.startupsimulator.model.StartupEvent;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.realtime.SseService;
import com.startupsimulator.repository.StartupEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Central place to append a {@link StartupEvent} to the log and fan it out to
 * connected clients. Every meaningful state change in the simulation should go
 * through {@link #record}.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final StartupEventRepository eventRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @Transactional
    public StartupEventDto record(Long startupId, EventType type, String message, Map<String, Object> payload) {
        String payloadJson = serialize(payload);
        StartupEvent saved = eventRepository.save(new StartupEvent(startupId, type, message, payloadJson));
        StartupEventDto dto = StartupEventDto.from(saved);
        sseService.broadcast(startupId, dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<StartupEventDto> history(Long startupId) {
        return eventRepository.findByStartupIdOrderById(startupId).stream()
                .map(StartupEventDto::from)
                .toList();
    }

    private String serialize(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
