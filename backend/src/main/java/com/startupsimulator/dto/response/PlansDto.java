package com.startupsimulator.dto.response;

import com.startupsimulator.model.Budget;
import com.startupsimulator.model.MarketingPlan;
import com.startupsimulator.model.TechnicalPlan;

/**
 * Aggregates the three department plans (Development / Marketing / Finance) plus
 * their per-analysis metadata into a single snapshot the frontend can fetch with
 * one call to {@code GET /api/startups/{id}/plans}. Any plan may be null if that
 * department has not produced an analysis yet.
 */
public record PlansDto(
        TechnicalPlanDto technical,
        MarketingPlanDto marketing,
        BudgetDto budget
) {
    public static PlansDto of(TechnicalPlan technical, MarketingPlan marketing, Budget budget) {
        return new PlansDto(
                TechnicalPlanDto.from(technical),
                MarketingPlanDto.from(marketing),
                BudgetDto.from(budget));
    }
}
