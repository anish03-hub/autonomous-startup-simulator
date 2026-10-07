package com.startupsimulator.dto.response;

import java.util.List;

public record MvpSectionDto(
        List<MvpFeatureDetailDto> mvpFeatures,
        List<MvpFeatureDetailDto> deferredFeatures,
        String source
) {
}
