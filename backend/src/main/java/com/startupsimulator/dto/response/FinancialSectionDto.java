package com.startupsimulator.dto.response;

import java.util.List;
import java.util.Map;

public record FinancialSectionDto(
        Map<String, Double> startupCosts,
        double startingCapital,
        double totalUpfrontCost,
        double monthlyOperatingCosts,
        double monthlyBurn,
        Map<String, String> budgetAllocation,
        String pricingModel,
        List<String> revenueAssumptions,
        double runwayMonths,
        String breakEvenConsiderations,
        List<String> financialRisks,
        String source
) {
}
