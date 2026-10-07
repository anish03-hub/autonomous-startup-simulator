package com.startupsimulator.dto.response;

import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.SimulationPhase;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Full view of a startup including shared narrative and health metrics. */
public record StartupDto(
        Long id,
        String name,
        String originalIdea,
        SimulationPhase currentPhase,
        String problem,
        String solution,
        String targetAudience,
        String valueProposition,
        String businessModel,
        String constraints,
        // ---- CEO strategic analysis (Phase 2A) ----
        String executiveSummary,
        List<String> strategicObjectives,
        List<String> assumptions,
        String mvpDirection,
        String ceoAnalysisProvider,
        boolean ceoAnalysisFailed,
        String ceoAnalysisError,
        StartupHealthDto health,
        boolean simulationStarted,
        boolean simulationCompleted,
        Instant createdAt,
        Instant updatedAt
) {
    public static StartupDto from(Startup s) {
        return new StartupDto(
                s.getId(), s.getName(), s.getOriginalIdea(), s.getCurrentPhase(),
                s.getProblem(), s.getSolution(), s.getTargetAudience(), s.getValueProposition(),
                s.getBusinessModel(), s.getConstraints(),
                s.getExecutiveSummary(),
                lines(s.getStrategicObjectives()),
                lines(s.getAssumptions()),
                s.getMvpDirection(),
                s.getCeoAnalysisProvider(),
                s.isCeoAnalysisFailed(),
                s.getCeoAnalysisError(),
                StartupHealthDto.from(s),
                s.isSimulationStarted(), s.isSimulationCompleted(),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    /** Split a newline-joined text column into a clean list for the frontend. */
    private static List<String> lines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();
    }
}
