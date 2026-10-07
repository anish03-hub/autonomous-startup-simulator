package com.startupsimulator.model;

import com.startupsimulator.model.enums.AgentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A message authored by an agent. When {@code debateId} is set the message
 * belongs to a boardroom debate transcript; otherwise it is a standalone
 * status update shown in the department view.
 */
@Entity
@Table(name = "agent_messages")
@Getter
@Setter
@NoArgsConstructor
public class AgentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 32)
    private AgentType agentType;

    @Column(name = "debate_id")
    private Long debateId;

    /**
     * Which debate round produced this message (Phase 2C: 1=POSITION, 2=CHALLENGE,
     * 3=CONVERGENCE). Null for non-debate status messages.
     */
    @Column(name = "debate_round")
    private Integer debateRound;

    /**
     * The kind of debate turn: POSITION, CHALLENGE, CONVERGENCE, FRAMING or
     * SYNTHESIS (Phase 2C). Null for non-debate status messages.
     */
    @Column(name = "message_type", length = 24)
    private String messageType;

    /**
     * The agent this message is addressed to. For a CHALLENGE debate turn it is
     * the department being challenged (Phase 2C); for a Phase 3 direct message it
     * is the recipient the sender chose. Null for a broadcast / status message.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_agent", length = 32)
    private AgentType targetAgent;

    /**
     * Optional short subject line for a Phase 3 addressed message (the sender's
     * chosen topic). Null for debate turns and status messages. Nullable column —
     * added by {@code ddl-auto}, no migration (no migration tooling in this
     * project; see application.yml).
     */
    @Column(name = "subject", length = 160)
    private String subject;

    /**
     * Read-state for a Phase 3 addressed message: {@code true} once the recipient
     * agent has retrieved it from its inbox and folded it into its reasoning
     * context, so it is not delivered twice. Debate turns and status messages
     * leave this {@code false}. Nullable-safe primitive, default {@code false}.
     */
    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AgentMessage(Long startupId, AgentType agentType, String content) {
        this.startupId = startupId;
        this.agentType = agentType;
        this.content = content;
    }

    /**
     * Convenience constructor for a Phase 3 addressed message: a {@code sender}
     * addressing a {@code targetAgent} with an optional {@code subject} and body.
     */
    public AgentMessage(Long startupId, AgentType sender, AgentType targetAgent,
                        String subject, String content) {
        this.startupId = startupId;
        this.agentType = sender;
        this.targetAgent = targetAgent;
        this.subject = subject;
        this.content = content;
    }
}
