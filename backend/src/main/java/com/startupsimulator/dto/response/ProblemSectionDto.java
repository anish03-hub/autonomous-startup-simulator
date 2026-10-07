package com.startupsimulator.dto.response;

import java.util.List;

public record ProblemSectionDto(
        String problemStatement,
        String targetCustomer,
        List<String> customerPainPoints,
        List<String> currentAlternatives,
        List<String> assumptions,
        String source
) {
}
