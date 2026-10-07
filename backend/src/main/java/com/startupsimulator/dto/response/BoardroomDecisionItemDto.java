package com.startupsimulator.dto.response;

import java.util.List;

public record BoardroomDecisionItemDto(
        Long id,
        String decision,
        String finalStatus,
        List<String> supportingArguments,
        List<String> opposingArguments,
        List<String> departmentsInvolved,
        String ceoResolution,
        String reason
) {
}
