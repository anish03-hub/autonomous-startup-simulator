package com.startupsimulator.dto.response;

import java.util.List;

public record BoardroomDecisionsSectionDto(
        List<String> disagreements,
        List<String> debateChanges,
        String ceoDecisionSummary,
        List<BoardroomDecisionItemDto> decisions,
        String source
) {
}
