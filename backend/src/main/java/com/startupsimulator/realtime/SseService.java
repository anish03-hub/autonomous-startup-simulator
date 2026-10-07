package com.startupsimulator.realtime;

import com.startupsimulator.dto.response.StartupEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Server-Sent Events streams, one group of subscribers per startup.
 * The simulation pushes {@link StartupEventDto}s here and every connected
 * client (the office UI) receives them in real time. SSE is used instead of
 * raw WebSocket for Phase 1 because it is simpler and needs no extra client
 * library while satisfying the real-time requirement.
 */
@Slf4j
@Service
public class SseService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final Map<Long, List<SseEmitter>> emittersByStartup = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long startupId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByStartup.computeIfAbsent(startupId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(startupId, emitter));
        emitter.onTimeout(() -> remove(startupId, emitter));
        emitter.onError(e -> remove(startupId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("subscribed to startup " + startupId));
        } catch (IOException e) {
            remove(startupId, emitter);
        }
        return emitter;
    }

    public void broadcast(Long startupId, StartupEventDto event) {
        List<SseEmitter> emitters = emittersByStartup.get(startupId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("startup-event").data(event));
            } catch (Exception e) {
                remove(startupId, emitter);
            }
        }
    }

    private void remove(Long startupId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByStartup.get(startupId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
