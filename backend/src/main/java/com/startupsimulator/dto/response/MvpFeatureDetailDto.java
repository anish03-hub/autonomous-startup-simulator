package com.startupsimulator.dto.response;

public record MvpFeatureDetailDto(
        Long id,
        String name,
        String description,
        boolean inMvp,
        String targetRelease,
        int effort,
        String priority,
        String reason,
        String sourceDepartment
) {
}
