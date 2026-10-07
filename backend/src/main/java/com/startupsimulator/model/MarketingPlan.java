package com.startupsimulator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Marketing department's plan. One row per startup in Phase 1. */
@Entity
@Table(name = "marketing_plans")
@Getter
@Setter
@NoArgsConstructor
public class MarketingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false, unique = true)
    private Long startupId;

    @Column(columnDefinition = "TEXT")
    private String positioning;

    @Column(columnDefinition = "TEXT")
    private String pricingStrategy;

    /** Comma-separated acquisition channels. */
    @Column(name = "channels", columnDefinition = "TEXT")
    private String channels;

    @Column(name = "competitor_analysis", columnDefinition = "TEXT")
    private String competitorAnalysis;

    @Column(name = "go_to_market", columnDefinition = "TEXT")
    private String goToMarket;

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

    public MarketingPlan(Long startupId) {
        this.startupId = startupId;
    }
}
