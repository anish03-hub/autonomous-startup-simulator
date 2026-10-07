package com.startupsimulator.dto.response;

public record ReasoningStepDto(
        int stepNumber,
        String stage,
        String title,
        String description,
        String department
) {
}
