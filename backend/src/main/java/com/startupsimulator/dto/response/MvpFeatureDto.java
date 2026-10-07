package com.startupsimulator.dto.response;

import com.startupsimulator.model.MvpFeature;

public record MvpFeatureDto(
        Long id,
        String name,
        String description,
        boolean inMvp,
        String targetRelease,
        int effort
) {
    public static MvpFeatureDto from(MvpFeature f) {
        return new MvpFeatureDto(f.getId(), f.getName(), f.getDescription(),
                f.isInMvp(), f.getTargetRelease(), f.getEffort());
    }
}
