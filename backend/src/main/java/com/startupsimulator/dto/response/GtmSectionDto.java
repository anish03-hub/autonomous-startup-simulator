package com.startupsimulator.dto.response;

import java.util.List;

public record GtmSectionDto(
        String targetSegment,
        String persona,
        String positioning,
        String messaging,
        List<String> acquisitionChannels,
        String launchStrategy,
        List<String> initialLaunchPlan,
        List<String> marketingRisks,
        List<String> assumptions,
        String source
) {
}
