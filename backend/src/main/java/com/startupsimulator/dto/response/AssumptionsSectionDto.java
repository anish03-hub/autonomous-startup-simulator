package com.startupsimulator.dto.response;

import java.util.List;

public record AssumptionsSectionDto(
        List<AssumptionItemDto> aggregatedAssumptions,
        String source
) {
}
