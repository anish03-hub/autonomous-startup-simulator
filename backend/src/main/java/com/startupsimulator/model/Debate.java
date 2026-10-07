package com.startupsimulator.model;

import com.startupsimulator.model.enums.DebateStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A boardroom debate about a specific topic between the agents. */
@Entity
@Table(name = "debates")
@Getter
@Setter
@NoArgsConstructor
public class Debate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DebateStatus status = DebateStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    private Instant resolvedAt;

    public Debate(Long startupId, String topic, String question) {
        this.startupId = startupId;
        this.topic = topic;
        this.question = question;
    }
}
