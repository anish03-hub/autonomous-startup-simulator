package com.startupsimulator.model;

import com.startupsimulator.model.enums.AgentState;
import com.startupsimulator.model.enums.AgentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** An AI department employee working inside a startup. */
@Entity
@Table(name = "agents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"startup_id", "type"}))
@Getter
@Setter
@NoArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "startup_id", nullable = false)
    private Startup startup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentType type;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentState state = AgentState.IDLE;

    /** Short human-readable line describing what the agent is doing right now. */
    @Column(name = "current_activity", columnDefinition = "TEXT")
    private String currentActivity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Agent(Startup startup, AgentType type, String name) {
        this.startup = startup;
        this.type = type;
        this.name = name;
    }
}
