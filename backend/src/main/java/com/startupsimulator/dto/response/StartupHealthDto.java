package com.startupsimulator.dto.response;

import com.startupsimulator.model.Startup;

/** The "Startup Health" right-sidebar panel. */
public record StartupHealthDto(
        int overallProgress,
        int mvpProgress,
        int technicalFeasibility,
        int marketReadiness,
        int financialHealth,
        double budgetRemaining,
        double runwayMonths
) {
    public static StartupHealthDto from(Startup s) {
        return new StartupHealthDto(
                s.getOverallProgress(), s.getMvpProgress(), s.getTechnicalFeasibility(),
                s.getMarketReadiness(), s.getFinancialHealth(),
                s.getBudgetRemaining(), s.getRunwayMonths());
    }
}
