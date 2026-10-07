package com.startupsimulator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Development department's technical plan. One row per startup in Phase 1. */
@Entity
@Table(name = "technical_plans")
@Getter
@Setter
@NoArgsConstructor
public class TechnicalPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false, unique = true)
    private Long startupId;

    @Column(columnDefinition = "TEXT")
    private String architecture;

    /** Comma-separated technology choices. */
    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @Column(name = "technical_risks", columnDefinition = "TEXT")
    private String technicalRisks;

    /** Rough estimate of engineering months for the MVP. */
    private double estimatedEngineeringMonths = 0d;

    // ---- Analysis provenance (Phase 2B) -------------------------------------
    /** Provider that produced this plan ("mock", "openai", or "fallback"). */
    @Column(name = "analysis_provider", length = 32)
    private String analysisProvider;

    /** True when the last real analysis attempt failed and a fallback was used. */
    @Column(name = "analysis_failed")
    private boolean analysisFailed = false;

    /** Short, non-sensitive description of the last analysis failure. */
    @Column(name = "analysis_error", columnDefinition = "TEXT")
    private String analysisError;

    public TechnicalPlan(Long startupId) {
        this.startupId = startupId;
    }
}
