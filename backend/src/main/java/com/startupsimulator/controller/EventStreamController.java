package com.startupsimulator.controller;

import com.startupsimulator.realtime.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events endpoint. The frontend opens one long-lived connection per
 * startup and receives {@code startup-event} messages as the simulation runs.
 */
@RestController
@RequestMapping("/api/startups")
@RequiredArgsConstructor
public class EventStreamController {

    private final SseService sseService;

    @GetMapping("/{id}/events/stream")
    public SseEmitter stream(@PathVariable Long id) {
        return sseService.subscribe(id);
    }
}
