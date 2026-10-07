package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strongly-typed output of a single agent's debate turn (Phase 2C). Produced as
 * JSON structured output by the LLM through the {@link LLMService} seam — never
 * parsed from free-form markdown — or built deterministically as a fallback when
 * no real provider is configured or a turn fails.
 *
 * <p>Every field is optional at the wire level (a model may omit some); the
 * turn is considered usable as long as it carries either a {@code position} or a
 * {@code recommendation}. {@link #normalized()} trims and null-guards the rest.
 *
 * <p>Unknown properties are ignored so a slightly over-eager model response does
 * not break parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebateResponse(
        /** This agent's stance in one or two sentences. */
        String position,
        /** Why it holds that stance, grounded in its own analysis. */
        String reasoning,
        /** Assumptions or claims by OTHER departments it is challenging (round 2). */
        List<String> challenges,
        /** Points it concedes to others (round 3 convergence). */
        List<String> concessions,
        /** Its concrete recommendation for the MVP / scope / launch. */
        String recommendation,
        /** Self-reported confidence 0-100. */
        Integer confidence,
        /** Remaining concerns it wants on the record. */
        List<String> concerns,
        /**
         * Phase 3: the department this turn is addressing (by AgentType name or
         * display name). On a CHALLENGE turn the model may name whom it is
         * challenging; the boardroom validates it and, when valid, records it as
         * the message's target. Null (legacy / non-challenge turns) means the
         * boardroom keeps its default seating-based target.
         */
        String challengeTarget
) {

    /**
     * Legacy constructor (pre-Phase-3 arity) with no explicit challenge target.
     * Keeps the deterministic fallbacks and tests compiling unchanged; Jackson
     * still binds the canonical constructor.
     */
    public DebateResponse(
            String position,
            String reasoning,
            List<String> challenges,
            List<String> concessions,
            String recommendation,
            Integer confidence,
            List<String> concerns) {
        this(position, reasoning, challenges, concessions, recommendation, confidence, concerns, null);
    }

    /** A turn is usable if it carries a position or a recommendation. */
    public boolean isValid() {
        return notBlank(position) || notBlank(recommendation);
    }

    /** Trimmed strings, empty lists for nulls, confidence clamped to 0-100. */
    public DebateResponse normalized() {
        int c = confidence == null ? 60 : Math.max(0, Math.min(100, confidence));
        return new DebateResponse(
                trim(position),
                trim(reasoning),
                orEmpty(challenges),
                orEmpty(concessions),
                trim(recommendation),
                c,
                orEmpty(concerns),
                trim(challengeTarget));
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
}
