package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strongly-typed CEO synthesis of the whole boardroom debate (Phase 2C). The CEO
 * reads all four department analyses AND the full debate transcript, weighs the
 * unresolved disagreements, and produces the final decision. Produced as JSON
 * structured output through the {@link LLMService} seam, or deterministically as
 * a fallback so the simulation always reaches a decision.
 *
 * <p>{@code deferredFeatures} is the single structured lever the synthesis uses
 * to reshape the MVP: the orchestrator matches these names against the proposed
 * features and moves them to V2. This keeps the blueprint an authoritative
 * reflection of the debate outcome (§11) without any markdown parsing.
 *
 * <p>Unknown properties are ignored so a slightly over-eager model response does
 * not break parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionSynthesisResponse(
        /** The headline decision. */
        String decision,
        /** Why the CEO decided this way, referencing the debate. */
        String rationale,
        /** Debate arguments the CEO accepted. */
        List<String> acceptedArguments,
        /** Debate arguments the CEO rejected. */
        List<String> rejectedArguments,
        /** The final MVP direction to build. */
        String finalMvpDirection,
        /** Names of features to defer to V2 (matched against the proposed MVP features). */
        List<String> deferredFeatures,
        /** The final top risks. */
        List<String> finalRisks,
        /** The final priorities. */
        List<String> finalPriorities
) {

    /** A synthesis is usable if it carries a decision and a rationale. */
    public boolean isValid() {
        return notBlank(decision) && notBlank(rationale);
    }

    /** Trimmed strings and empty lists for nulls. */
    public DecisionSynthesisResponse normalized() {
        return new DecisionSynthesisResponse(
                trim(decision),
                trim(rationale),
                orEmpty(acceptedArguments),
                orEmpty(rejectedArguments),
                trim(finalMvpDirection),
                orEmpty(deferredFeatures),
                orEmpty(finalRisks),
                orEmpty(finalPriorities));
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
