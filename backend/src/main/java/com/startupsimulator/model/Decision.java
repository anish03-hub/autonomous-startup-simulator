package com.startupsimulator.model;

import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.DecisionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A decision proposed (usually by the CEO) to resolve a debate. */
@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "debate_id")
    private Long debateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decided_by", nullable = false, length = 32)
    private AgentType decidedBy = AgentType.CEO;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Comma-separated affected department names, kept simple for Phase 1. */
    @Column(name = "affected_departments")
    private String affectedDepartments;

    // ---- Phase 2C: structured CEO synthesis of the boardroom debate ----------

    /** The final MVP direction the CEO settled on after the debate. */
    @Column(name = "final_mvp_direction", columnDefinition = "TEXT")
    private String finalMvpDirection;

    /** Debate arguments the CEO accepted, newline-joined. */
    @Column(name = "accepted_arguments", columnDefinition = "TEXT")
    private String acceptedArguments;

    /** Debate arguments the CEO rejected, newline-joined. */
    @Column(name = "rejected_arguments", columnDefinition = "TEXT")
    private String rejectedArguments;

    /** The final top risks after the debate, newline-joined. */
    @Column(name = "final_risks", columnDefinition = "TEXT")
    private String finalRisks;

    /** The final priorities after the debate, newline-joined. */
    @Column(name = "final_priorities", columnDefinition = "TEXT")
    private String finalPriorities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionStatus status = DecisionStatus.PROPOSED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Decision(Long startupId, Long debateId, String decision, String reason) {
        this.startupId = startupId;
        this.debateId = debateId;
        this.decision = decision;
        this.reason = reason;
    }
}
