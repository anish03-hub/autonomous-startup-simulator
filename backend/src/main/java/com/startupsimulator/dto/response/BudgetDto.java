package com.startupsimulator.dto.response;

import com.startupsimulator.model.Budget;

public record BudgetDto(
        double developmentCost,
        double infrastructureCost,
        double marketingBudget,
        double operatingCost,
        double totalUpfrontCost,
        double startingCapital,
        double monthlyBurn,
        double projectedMonthlyRevenue,
        double runwayMonths,
        String breakEvenAssumption,
        String analysisProvider,
        boolean analysisFailed,
        String analysisError
) {
    public static BudgetDto from(Budget b) {
        if (b == null) {
            return null;
        }
        return new BudgetDto(b.getDevelopmentCost(), b.getInfrastructureCost(), b.getMarketingBudget(),
                b.getOperatingCost(), b.totalUpfrontCost(), b.getStartingCapital(), b.getMonthlyBurn(),
                b.getProjectedMonthlyRevenue(), b.getRunwayMonths(), b.getBreakEvenAssumption(),
                b.getAnalysisProvider(), b.isAnalysisFailed(), b.getAnalysisError());
    }
}
