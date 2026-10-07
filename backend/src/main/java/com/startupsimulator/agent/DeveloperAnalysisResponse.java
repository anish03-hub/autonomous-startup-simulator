package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strongly-typed Development analysis, produced as JSON structured output by the
 * LLM (or deterministically by the fallback). The Development department turns
 * the CEO's direction into a concrete MVP scope, architecture, stack, timeline
 * and technical-risk view; this record is validated before any of it is written
 * to the authoritative state.
 *
 * <p>Unknown properties are ignored so a slightly over-eager model response does
 * not break parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeveloperAnalysisResponse(
        String architecture,
        List<String> techStack,
        String timeline,
        List<String> technicalRisks,
        double estimatedEngineeringMonths,
        Integer technicalFeasibility,
        String solutionSummary,
        List<FeatureProposal> mvpFeatures,
        /**
         * Phase 3: an optional communication intent the model may include to
         * address another department (e.g. Finance). Null when the model chose
         * not to message anyone. This is a field of the structured response — NOT
         * a tool/function call.
         */
        AgentMessageIntent messageIntent,
        /**
         * Phase 5B: optional, agent-driven memory-creation intents. The model may
         * include a few durable, future-useful memories to persist (never a
         * restatement of this analysis, never generic filler). Empty/absent when
         * the model chose to record nothing. Persisted — bounded, validated and
         * de-duplicated — only after a successful analysis, through MemoryService.
         */
        List<AgentMemoryIntent> memoryIntents
) {

    /**
     * Legacy constructor (pre-Phase-3 arity) with no communication intent.
     * Keeps existing call-sites and tests compiling unchanged; Jackson still
     * binds the canonical constructor.
     */
    public DeveloperAnalysisResponse(
            String architecture,
            List<String> techStack,
            String timeline,
            List<String> technicalRisks,
            double estimatedEngineeringMonths,
            Integer technicalFeasibility,
            String solutionSummary,
            List<FeatureProposal> mvpFeatures) {
        this(architecture, techStack, timeline, technicalRisks, estimatedEngineeringMonths,
                technicalFeasibility, solutionSummary, mvpFeatures, null, List.of());
    }

    /**
     * Legacy constructor (Phase 3 arity) with a communication intent but no
     * memory intents. Keeps Phase 3 call-sites and tests compiling unchanged.
     */
    public DeveloperAnalysisResponse(
            String architecture,
            List<String> techStack,
            String timeline,
            List<String> technicalRisks,
            double estimatedEngineeringMonths,
            Integer technicalFeasibility,
            String solutionSummary,
            List<FeatureProposal> mvpFeatures,
            AgentMessageIntent messageIntent) {
        this(architecture, techStack, timeline, technicalRisks, estimatedEngineeringMonths,
                technicalFeasibility, solutionSummary, mvpFeatures, messageIntent, List.of());
    }

    /** A single proposed MVP feature. {@code effort} is a rough 1-5 scale. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeatureProposal(
            String name,
            String description,
            Boolean inMvp,
            Integer effort
    ) {
        boolean isValid() {
            return name != null && !name.isBlank();
        }

        FeatureProposal normalized() {
            return new FeatureProposal(
                    name == null ? null : name.trim(),
                    description == null ? "" : description.trim(),
                    inMvp == null ? Boolean.TRUE : inMvp,
                    effort == null ? 3 : Math.max(1, Math.min(5, effort)));
        }
    }

    /**
     * Structural validation. Require the core narrative fields plus a non-empty
     * stack and a positive engineering estimate; feature list and risks are
     * optional (normalised to empty).
     */
    public boolean isValid() {
        return notBlank(architecture)
                && notBlank(timeline)
                && notBlank(solutionSummary)
                && techStack != null && techStack.stream().anyMatch(DeveloperAnalysisResponse::notBlank)
                && estimatedEngineeringMonths > 0;
    }

    /** Return a normalised copy: trimmed strings, empty lists for nulls, clamped values. */
    public DeveloperAnalysisResponse normalized() {
        int feasibility = technicalFeasibility == null ? 75 : Math.max(0, Math.min(100, technicalFeasibility));
        return new DeveloperAnalysisResponse(
                trim(architecture),
                orEmpty(techStack),
                trim(timeline),
                orEmpty(technicalRisks),
                round1(estimatedEngineeringMonths),
                feasibility,
                trim(solutionSummary),
                orEmptyFeatures(mvpFeatures),
                messageIntent,
                orEmptyIntents(memoryIntents));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static List<String> orEmpty(List<String> list) {
        return list == null ? List.of()
                : list.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
    }

    private static List<FeatureProposal> orEmptyFeatures(List<FeatureProposal> list) {
        return list == null ? List.of()
                : list.stream().filter(f -> f != null && f.isValid()).map(FeatureProposal::normalized).toList();
    }

    private static List<AgentMemoryIntent> orEmptyIntents(List<AgentMemoryIntent> list) {
        return list == null ? List.of()
                : list.stream().filter(i -> i != null && i.isPresent()).toList();
    }
}
