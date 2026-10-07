package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strongly-typed Finance analysis, produced as JSON structured output by the LLM
 * (or deterministically by the fallback). Finance builds on the CEO direction,
 * the Development timeline and the Marketing pricing to model costs, burn,
 * revenue and runway; validated before it is written to state.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinanceAnalysisResponse(
        double developmentCost,
        double marketingBudget,
        double infrastructureCost,
        double operatingCost,
        Double startingCapital,
        double monthlyBurn,
        double projectedMonthlyRevenue,
        String breakEvenAssumption,
        Integer financialHealth,
        List<String> risks,
        /**
         * Phase 3: an optional communication intent the model may include to
         * address another department (e.g. a reply to Development). Null when the
         * model chose not to message anyone. A field of the structured response —
         * NOT a tool/function call.
         */
        AgentMessageIntent messageIntent,
        /**
         * Phase 5B: optional, agent-driven memory-creation intents (e.g. a
         * validated financial assumption or an unresolved runway risk worth
         * remembering). Empty/absent when nothing is worth recording. Persisted —
         * bounded, validated, de-duplicated — only after a successful analysis.
         */
        List<AgentMemoryIntent> memoryIntents
) {

    /**
     * Legacy constructor (pre-Phase-3 arity) with no communication intent.
     * Keeps existing call-sites and tests compiling unchanged; Jackson still
     * binds the canonical constructor.
     */
    public FinanceAnalysisResponse(
            double developmentCost,
            double marketingBudget,
            double infrastructureCost,
            double operatingCost,
            Double startingCapital,
            double monthlyBurn,
            double projectedMonthlyRevenue,
            String breakEvenAssumption,
            Integer financialHealth,
            List<String> risks) {
        this(developmentCost, marketingBudget, infrastructureCost, operatingCost, startingCapital,
                monthlyBurn, projectedMonthlyRevenue, breakEvenAssumption, financialHealth, risks, null, List.of());
    }

    /**
     * Legacy constructor (Phase 3 arity) with a communication intent but no
     * memory intents. Keeps Phase 3 call-sites and tests compiling unchanged.
     */
    public FinanceAnalysisResponse(
            double developmentCost,
            double marketingBudget,
            double infrastructureCost,
            double operatingCost,
            Double startingCapital,
            double monthlyBurn,
            double projectedMonthlyRevenue,
            String breakEvenAssumption,
            Integer financialHealth,
            List<String> risks,
            AgentMessageIntent messageIntent) {
        this(developmentCost, marketingBudget, infrastructureCost, operatingCost, startingCapital,
                monthlyBurn, projectedMonthlyRevenue, breakEvenAssumption, financialHealth, risks,
                messageIntent, List.of());
    }

    /** Require a positive development cost, a positive burn and a break-even note. */
    public boolean isValid() {
        return developmentCost > 0
                && monthlyBurn > 0
                && notBlank(breakEvenAssumption);
    }

    public FinanceAnalysisResponse normalized() {
        int health = financialHealth == null ? 68 : Math.max(0, Math.min(100, financialHealth));
        double capital = (startingCapital == null || startingCapital <= 0) ? 60_000d : startingCapital;
        return new FinanceAnalysisResponse(
                round(developmentCost),
                round(marketingBudget),
                round(infrastructureCost),
                round(operatingCost),
                round(capital),
                round(monthlyBurn),
                round(projectedMonthlyRevenue),
                trim(breakEvenAssumption),
                health,
                orEmpty(risks),
                messageIntent,
                orEmptyIntents(memoryIntents));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static List<String> orEmpty(List<String> list) {
        return list == null ? List.of()
                : list.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
    }

    private static List<AgentMemoryIntent> orEmptyIntents(List<AgentMemoryIntent> list) {
        return list == null ? List.of()
                : list.stream().filter(i -> i != null && i.isPresent()).toList();
    }
}
