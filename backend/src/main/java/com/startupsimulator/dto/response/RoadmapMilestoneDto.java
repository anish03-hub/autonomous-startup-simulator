package com.startupsimulator.dto.response;

import com.startupsimulator.model.RoadmapMilestone;

public record RoadmapMilestoneDto(
        Long id,
        String phase,
        String title,
        String description,
        String timeframe
) {
    public static RoadmapMilestoneDto from(RoadmapMilestone r) {
        return new RoadmapMilestoneDto(r.getId(), r.getPhase(), r.getTitle(),
                r.getDescription(), r.getTimeframe());
    }
}
