package com.startupsimulator.tool.impl;

import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.tool.AbstractTool;
import com.startupsimulator.tool.ToolArguments;
import com.startupsimulator.tool.ToolParamType;
import com.startupsimulator.tool.ToolParameter;
import com.startupsimulator.tool.ToolSchema;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code financial_calculator} — deterministic startup cash maths.
 *
 * <p>Given upfront cost components, starting capital, and monthly expense/revenue,
 * it computes total upfront cost, net monthly burn, runway (months of cash left),
 * and break-even (months for monthly profit to recover the upfront investment).
 * All outputs are a pure function of the inputs — no randomness, no stored state.
 *
 * <p>The upfront-cost sum mirrors the existing {@code Budget.totalUpfrontCost()}
 * precedent (development + infrastructure + marketing + operating).
 */
@Component
public class FinancialCalculatorTool extends AbstractTool {

    @Override
    public String name() {
        return "financial_calculator";
    }

    @Override
    public String description() {
        return "Computes total upfront cost, net monthly burn, runway in months, and "
                + "break-even in months from a startup's cost structure, starting capital, "
                + "and monthly expenses/revenue.";
    }

    @Override
    public ToolSchema schema() {
        return new ToolSchema(name(), description(), List.of(
                ToolParameter.requiredRange("developmentCost", ToolParamType.NUMBER,
                        "One-off development cost.", 0.0, null),
                ToolParameter.requiredRange("infrastructureCost", ToolParamType.NUMBER,
                        "One-off infrastructure cost.", 0.0, null),
                ToolParameter.requiredRange("marketingBudget", ToolParamType.NUMBER,
                        "One-off marketing budget.", 0.0, null),
                ToolParameter.requiredRange("operatingCost", ToolParamType.NUMBER,
                        "One-off operating cost.", 0.0, null),
                ToolParameter.requiredRange("startingCapital", ToolParamType.NUMBER,
                        "Total cash available at the start.", 0.0, null),
                ToolParameter.requiredRange("monthlyExpenses", ToolParamType.NUMBER,
                        "Recurring monthly expenses (gross monthly burn).", 0.0, null),
                ToolParameter.optionalRange("monthlyRevenue", ToolParamType.NUMBER,
                        "Recurring monthly revenue (default 0).", 0.0, null)
        ));
    }

    @Override
    protected Object run(ToolArguments args, StartupContext context) {
        double developmentCost = requireNonNegative("developmentCost", args.requireDouble("developmentCost"));
        double infrastructureCost = requireNonNegative("infrastructureCost", args.requireDouble("infrastructureCost"));
        double marketingBudget = requireNonNegative("marketingBudget", args.requireDouble("marketingBudget"));
        double operatingCost = requireNonNegative("operatingCost", args.requireDouble("operatingCost"));
        double startingCapital = requireNonNegative("startingCapital", args.requireDouble("startingCapital"));
        double monthlyExpenses = requireNonNegative("monthlyExpenses", args.requireDouble("monthlyExpenses"));
        double monthlyRevenue = requireNonNegative("monthlyRevenue", args.optionalDouble("monthlyRevenue", 0.0));

        double totalUpfrontCost = developmentCost + infrastructureCost + marketingBudget + operatingCost;
        double capitalAfterUpfront = startingCapital - totalUpfrontCost;
        double monthlyNet = monthlyRevenue - monthlyExpenses; // positive => cash-flow positive
        double monthlyBurn = monthlyExpenses - monthlyRevenue; // positive => burning cash

        boolean cashFlowPositive = monthlyNet > 0;

        // Runway: months of cash left while burning. Null when not burning (unbounded).
        Double runwayMonths = null;
        if (monthlyBurn > 0) {
            double remaining = Math.max(0.0, capitalAfterUpfront);
            runwayMonths = remaining / monthlyBurn;
        }

        // Break-even: months for monthly profit to recover upfront cost. Null if never.
        Double breakEvenMonths = null;
        if (monthlyNet > 0) {
            breakEvenMonths = totalUpfrontCost / monthlyNet;
        }

        return new Output(totalUpfrontCost, capitalAfterUpfront, monthlyBurn,
                cashFlowPositive, runwayMonths, breakEvenMonths);
    }

    /** Typed, structured output payload for {@code financial_calculator}. */
    public record Output(
            double totalUpfrontCost,
            double capitalAfterUpfront,
            double monthlyBurn,
            boolean cashFlowPositive,
            Double runwayMonths,
            Double breakEvenMonths
    ) {
    }
}
