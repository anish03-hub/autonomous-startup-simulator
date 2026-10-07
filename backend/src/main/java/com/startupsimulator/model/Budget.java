package com.startupsimulator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Finance department's budget model for a startup. One active row per startup
 * in Phase 1. All monetary values are in whole currency units.
 */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false, unique = true)
    private Long startupId;

    private double developmentCost = 0d;
    private double infrastructureCost = 0d;
    private double marketingBudget = 0d;
    private double operatingCost = 0d;

    /** Total starting capital assumed for the runway calculation. */
    private double startingCapital = 0d;

    /** Estimated monthly burn. */
    private double monthlyBurn = 0d;

    /** Assumed monthly revenue at steady state. */
    private double projectedMonthlyRevenue = 0d;

    private double runwayMonths = 0d;

    private String breakEvenAssumption;

    // ---- Analysis provenance (Phase 2B) -------------------------------------
    /** Provider that produced this budget ("mock", "openai", or "fallback"). */
    @Column(name = "analysis_provider", length = 32)
    private String analysisProvider;

    /** True when the last real analysis attempt failed and a fallback was used. */
    @Column(name = "analysis_failed")
    private boolean analysisFailed = false;

    /** Short, non-sensitive description of the last analysis failure. */
    @Column(name = "analysis_error", columnDefinition = "TEXT")
    private String analysisError;

    public double totalUpfrontCost() {
        return developmentCost + infrastructureCost + marketingBudget + operatingCost;
    }

    public Budget(Long startupId) {
        this.startupId = startupId;
    }
}
