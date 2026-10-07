package com.startupsimulator.dto.response;

import com.startupsimulator.model.TechnicalPlan;

public record TechnicalPlanDto(
        String architecture,
        String techStack,
        String timeline,
        String technicalRisks,
        double estimatedEngineeringMonths,
        String analysisProvider,
        boolean analysisFailed,
        String analysisError
) {
    public static TechnicalPlanDto from(TechnicalPlan t) {
        if (t == null) {
            return null;
        }
        return new TechnicalPlanDto(t.getArchitecture(), t.getTechStack(), t.getTimeline(),
                t.getTechnicalRisks(), t.getEstimatedEngineeringMonths(),
                t.getAnalysisProvider(), t.isAnalysisFailed(), t.getAnalysisError());
    }
}
