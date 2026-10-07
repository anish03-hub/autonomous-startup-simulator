package com.startupsimulator.dto.response;

import java.util.List;

public record RisksSectionDto(
        List<RiskItemDto> aggregatedRisks,
        String source
) {
}
