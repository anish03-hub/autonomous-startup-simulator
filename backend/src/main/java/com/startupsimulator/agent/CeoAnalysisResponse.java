package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strongly-typed CEO analysis, produced as JSON structured output by the LLM
 * (or deterministically by the fallback). The CEO turns an ambiguous idea into
 * a coherent startup direction; this record is validated before any of it is
 * written to the authoritative {@link com.startupsimulator.model.Startup} state.
 *
 * <p>Unknown properties are ignored so a slightly over-eager model response
 * does not break parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CeoAnalysisResponse(
        String executiveSummary,
        String problem,
        String targetCustomer,
        String proposedSolution,
        String valueProposition,
        String businessModel,
        List<String> strategicObjectives,
        List<String> assumptions,
        List<String> opportunities,
        List<String> risks,
        String recommendedMvpDirection,
        List<RoadmapEntry> roadmap
) {

    /**
     * A single CEO-authored roadmap milestone. Optional in the response: when the
     * model supplies these they become the authoritative roadmap; when it omits
     * them the deterministic scaffold is used instead (never overwriting a model
     * roadmap).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoadmapEntry(String phase, String title, String description, String timeframe) {
        public boolean isUsable() {
            return (phase != null && !phase.isBlank()) || (title != null && !title.isBlank());
        }

        public RoadmapEntry trimmed() {
            return new RoadmapEntry(trim(phase), trim(title), trim(description), trim(timeframe));
        }
    }

    /**
     * Backward-compatible constructor for the 11 narrative/list fields that
     * predate the optional roadmap. Existing call sites (the deterministic
     * analysis and tests) keep compiling; the roadmap defaults to empty.
     */
    public CeoAnalysisResponse(
            String executiveSummary, String problem, String targetCustomer, String proposedSolution,
            String valueProposition, String businessModel, List<String> strategicObjectives,
            List<String> assumptions, List<String> opportunities, List<String> risks,
            String recommendedMvpDirection) {
        this(executiveSummary, problem, targetCustomer, proposedSolution, valueProposition,
                businessModel, strategicObjectives, assumptions, opportunities, risks,
                recommendedMvpDirection, List.of());
    }

    /**
     * Structural validation of a parsed response. We require the core narrative
     * fields to be present and non-blank; the list fields (including the optional
     * roadmap) are normalised to empty lists by {@link #normalized()}.
     */
    public boolean isValid() {
        return notBlank(executiveSummary)
                && notBlank(problem)
                && notBlank(targetCustomer)
                && notBlank(proposedSolution)
                && notBlank(valueProposition)
                && notBlank(businessModel)
                && notBlank(recommendedMvpDirection);
    }

    /** Return a copy with null list fields replaced by empty lists. */
    public CeoAnalysisResponse normalized() {
        return new CeoAnalysisResponse(
                trim(executiveSummary), trim(problem), trim(targetCustomer),
                trim(proposedSolution), trim(valueProposition), trim(businessModel),
                orEmpty(strategicObjectives), orEmpty(assumptions),
                orEmpty(opportunities), orEmpty(risks), trim(recommendedMvpDirection),
                orEmptyRoadmap(roadmap));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static List<String> orEmpty(List<String> list) {
        return list == null ? List.of()
                : list.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
    }

    private static List<RoadmapEntry> orEmptyRoadmap(List<RoadmapEntry> list) {
        return list == null ? List.of()
                : list.stream().filter(e -> e != null && e.isUsable()).map(RoadmapEntry::trimmed).toList();
    }
}
