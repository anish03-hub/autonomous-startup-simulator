package com.startupsimulator.dto.response;

import java.util.List;

public record SolutionSectionDto(
        String productDescription,
        String coreValueProposition,
        List<String> primaryUserWorkflow,
        List<String> keyDifferentiators,
        String source
) {
}
