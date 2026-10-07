package com.startupsimulator.dto.response;

import java.util.List;

public record TechnicalSectionDto(
        String architecture,
        String frontend,
        String backend,
        String database,
        String aiMl,
        String integrations,
        String infrastructure,
        List<String> technicalRisks,
        String developmentTimeline,
        double estimatedEngineeringMonths,
        String source
) {
}
