package com.startupsimulator.tool.impl;

import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.tool.AbstractTool;
import com.startupsimulator.tool.ToolArguments;
import com.startupsimulator.tool.ToolParamType;
import com.startupsimulator.tool.ToolParameter;
import com.startupsimulator.tool.ToolSchema;
import com.startupsimulator.tool.ToolValidationException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code development_effort_estimator} — deterministic effort maths from a list of
 * per-feature effort points (the same 1–5 scale as {@code MvpFeature.effort}).
 *
 * <p>It sums the effort points, converts them to person-hours and calendar days
 * for a given team size, and reports the average effort per feature. Pure function
 * of its inputs.
 */
@Component
public class DevelopmentEffortEstimatorTool extends AbstractTool {

    /** Guard against pathological inputs (bounded execution). */
    private static final int MAX_FEATURES = 1_000;
    private static final double HOURS_PER_WORKDAY = 8.0;

    @Override
    public String name() {
        return "development_effort_estimator";
    }

    @Override
    public String description() {
        return "Estimates development effort from a list of per-feature effort points "
                + "(1-5 each): total effort points, person-hours, calendar days for the "
                + "given engineer count, and average effort per feature.";
    }

    @Override
    public ToolSchema schema() {
        return new ToolSchema(name(), description(), List.of(
                ToolParameter.required("featureEfforts", ToolParamType.INTEGER_ARRAY,
                        "Effort points per feature; each value in [1, 5]."),
                ToolParameter.optionalRange("engineerCount", ToolParamType.INTEGER,
                        "Number of engineers working in parallel (default 1).", 1.0, 100.0),
                ToolParameter.optionalRange("hoursPerPoint", ToolParamType.NUMBER,
                        "Person-hours per effort point (default 8).", 0.0, null)
        ));
    }

    @Override
    protected Object run(ToolArguments args, StartupContext context) {
        List<Integer> efforts = args.requireIntList("featureEfforts");
        if (efforts.isEmpty()) {
            throw new ToolValidationException("OUT_OF_RANGE",
                    "Argument 'featureEfforts' must contain at least one feature.");
        }
        if (efforts.size() > MAX_FEATURES) {
            throw new ToolValidationException("OUT_OF_RANGE",
                    "Argument 'featureEfforts' must contain at most " + MAX_FEATURES + " features.");
        }

        int totalEffortPoints = 0;
        for (int e : efforts) {
            inRange("featureEfforts[]", e, 1, 5);
            totalEffortPoints += e;
        }

        int engineerCount = (int) inRange("engineerCount", args.optionalInt("engineerCount", 1), 1, 100);
        double hoursPerPoint = requirePositive("hoursPerPoint", args.optionalDouble("hoursPerPoint", HOURS_PER_WORKDAY));

        int featureCount = efforts.size();
        double estimatedPersonHours = totalEffortPoints * hoursPerPoint;
        double estimatedCalendarDays = estimatedPersonHours / (engineerCount * HOURS_PER_WORKDAY);
        double averageEffortPerFeature = (double) totalEffortPoints / featureCount;

        return new Output(featureCount, totalEffortPoints, estimatedPersonHours,
                estimatedCalendarDays, averageEffortPerFeature);
    }

    /** Typed, structured output payload for {@code development_effort_estimator}. */
    public record Output(
            int featureCount,
            int totalEffortPoints,
            double estimatedPersonHours,
            double estimatedCalendarDays,
            double averageEffortPerFeature
    ) {
    }
}
