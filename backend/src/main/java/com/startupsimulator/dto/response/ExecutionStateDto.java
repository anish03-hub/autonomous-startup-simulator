package com.startupsimulator.dto.response;

import com.startupsimulator.model.enums.SimulationPhase;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ExecutionStateDto(
        Long startupId,
        SimulationPhase phase,
        int day,
        int week,
        boolean isRunning,
        boolean isPaused,
        boolean isCompleted,
        double overallProgress,
        double mvpProgress,
        double technicalProgress,
        double marketReadiness,
        double financialHealth,
        double budgetRemaining,
        double runwayMonths,
        int activeBlockerCount,
        Map<String, Double> departmentProgress,
        List<ExecutionTaskDto> tasks
) {}
