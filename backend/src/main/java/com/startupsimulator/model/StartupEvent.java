package com.startupsimulator.model;

import com.startupsimulator.model.enums.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An append-only record of something that happened in a startup's simulation.
 * These rows are streamed to the frontend (via SSE) to drive the living-office
 * UI, and can be replayed to reconstruct the timeline.
 */
@Entity
@Table(name = "startup_events", indexes = @Index(name = "idx_events_startup", columnList = "startup_id"))
@Getter
@Setter
@NoArgsConstructor
public class StartupEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType type;

    /** Human-readable summary shown in notifications / activity feeds. */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** JSON payload with event-specific data for the frontend. */
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StartupEvent(Long startupId, EventType type, String message, String payload) {
        this.startupId = startupId;
        this.type = type;
        this.message = message;
        this.payload = payload;
    }
}
