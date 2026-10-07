package com.startupsimulator.dto.response;

import java.util.List;

public record ExecutionPlan90DayDto(
        String overview,
        List<ExecutionStageDto> stages,
        String source
) {
}
