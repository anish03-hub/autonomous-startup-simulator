package com.startupsimulator.dto.response;

import java.util.List;

public record ExecutionStageDto(
        String stageName,
        String timeframe,
        String focus,
        List<RoadmapMilestoneDto> milestones,
        List<String> keyDeliverables,
        List<String> responsibleDepartments
) {
}
