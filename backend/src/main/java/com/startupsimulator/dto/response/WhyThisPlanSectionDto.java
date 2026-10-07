package com.startupsimulator.dto.response;

import java.util.List;

public record WhyThisPlanSectionDto(
        String summary,
        List<ReasoningStepDto> reasoningChain,
        String source
) {
}
