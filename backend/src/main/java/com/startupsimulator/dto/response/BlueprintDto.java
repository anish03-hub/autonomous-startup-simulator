package com.startupsimulator.dto.response;

import lombok.Builder;
import java.util.List;

/**
 * The final startup blueprint, aggregating the work of every department into a
 * single investor-ready document (see {@code /blueprint} endpoint).
 */
@Builder
public record BlueprintDto(
        Long startupId,
        String name,
        String pitch,
        String problem,
        String solution,
        String targetCustomer,
        String valueProposition,
        String businessModel,
        List<MvpFeatureDto> mvp,
        List<MvpFeatureDto> backlog,
        TechnicalPlanDto technical,
        MarketingPlanDto marketing,
        BudgetDto budget,
        String revenueModel,
        List<RoadmapMilestoneDto> roadmap,
        List<String> risks,
        List<DecisionDto> decisions,
        boolean complete,
        // ---- Phase 2D: Rich, traceable blueprint sections --------------------
        ExecutiveSummarySectionDto executiveSummarySection,
        ProblemSectionDto problemSection,
        SolutionSectionDto solutionSection,
        MvpSectionDto mvpSection,
        TechnicalSectionDto technicalSection,
        GtmSectionDto gtmSection,
        FinancialSectionDto financialSection,
        BoardroomDecisionsSectionDto boardroomSection,
        WhyThisPlanSectionDto whyThisPlanSection,
        RisksSectionDto risksSection,
        AssumptionsSectionDto assumptionsSection,
        ExecutionPlan90DayDto executionPlanSection
) {
    /** Overloaded constructor for backwards-compatibility. */
    public BlueprintDto(
            Long startupId,
            String name,
            String pitch,
            String problem,
            String solution,
            String targetCustomer,
            String valueProposition,
            String businessModel,
            List<MvpFeatureDto> mvp,
            List<MvpFeatureDto> backlog,
            TechnicalPlanDto technical,
            MarketingPlanDto marketing,
            BudgetDto budget,
            String revenueModel,
            List<RoadmapMilestoneDto> roadmap,
            List<String> risks,
            List<DecisionDto> decisions,
            boolean complete
    ) {
        this(
                startupId, name, pitch, problem, solution, targetCustomer, valueProposition, businessModel,
                mvp, backlog, technical, marketing, budget, revenueModel, roadmap, risks, decisions, complete,
                null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
