package com.startupsimulator.dto.response;

import java.util.List;

public record ExecutiveSummarySectionDto(
        String startupName,
        String pitch,
        String problem,
        String solution,
        String targetCustomer,
        String valueProposition,
        String businessModel,
        List<String> strategicObjectives,
        String mvpDirection,
        String source
) {
}
