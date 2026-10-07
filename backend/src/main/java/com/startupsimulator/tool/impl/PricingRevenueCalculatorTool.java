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
 * {@code pricing_revenue_calculator} — deterministic revenue maths.
 *
 * <p>From a unit price, a pool of potential customers, a conversion rate, and an
 * optional number of recurring billing periods per year, it computes the number
 * of paying customers, revenue per period, and annualised revenue. Pure function
 * of its inputs.
 */
@Component
public class PricingRevenueCalculatorTool extends AbstractTool {

    @Override
    public String name() {
        return "pricing_revenue_calculator";
    }

    @Override
    public String description() {
        return "Computes paying customers, revenue per billing period, and annual revenue "
                + "from unit price, potential customers, conversion rate, and recurring "
                + "periods per year.";
    }

    @Override
    public ToolSchema schema() {
        return new ToolSchema(name(), description(), List.of(
                ToolParameter.requiredRange("pricePerUnit", ToolParamType.NUMBER,
                        "Price charged per paying customer per period.", 0.0, null),
                ToolParameter.requiredRange("potentialCustomers", ToolParamType.INTEGER,
                        "Size of the addressable customer pool.", 0.0, null),
                ToolParameter.requiredRange("conversionRate", ToolParamType.NUMBER,
                        "Fraction of potential customers who pay, in [0, 1].", 0.0, 1.0),
                ToolParameter.optionalRange("recurringPeriodsPerYear", ToolParamType.INTEGER,
                        "Billing periods per year (e.g. 12 for monthly; default 1).", 1.0, 365.0)
        ));
    }

    @Override
    protected Object run(ToolArguments args, StartupContext context) {
        double pricePerUnit = requireNonNegative("pricePerUnit", args.requireDouble("pricePerUnit"));
        int potentialCustomers = (int) requireNonNegative("potentialCustomers", args.requireInt("potentialCustomers"));
        double conversionRate = inRange("conversionRate", args.requireDouble("conversionRate"), 0.0, 1.0);
        int recurringPeriodsPerYear = (int) inRange("recurringPeriodsPerYear",
                args.optionalInt("recurringPeriodsPerYear", 1), 1, 365);

        long payingCustomers = Math.round(potentialCustomers * conversionRate);
        double revenuePerPeriod = payingCustomers * pricePerUnit;
        double annualRevenue = revenuePerPeriod * recurringPeriodsPerYear;

        return new Output(payingCustomers, revenuePerPeriod, annualRevenue, recurringPeriodsPerYear);
    }

    /** Typed, structured output payload for {@code pricing_revenue_calculator}. */
    public record Output(
            long payingCustomers,
            double revenuePerPeriod,
            double annualRevenue,
            int recurringPeriodsPerYear
    ) {
    }
}
