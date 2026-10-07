package com.startupsimulator.dto.response;

import com.startupsimulator.model.MarketingPlan;

public record MarketingPlanDto(
        String positioning,
        String pricingStrategy,
        String channels,
        String competitorAnalysis,
        String goToMarket,
        String analysisProvider,
        boolean analysisFailed,
        String analysisError
) {
    public static MarketingPlanDto from(MarketingPlan m) {
        if (m == null) {
            return null;
        }
        return new MarketingPlanDto(m.getPositioning(), m.getPricingStrategy(), m.getChannels(),
                m.getCompetitorAnalysis(), m.getGoToMarket(),
                m.getAnalysisProvider(), m.isAnalysisFailed(), m.getAnalysisError());
    }
}
