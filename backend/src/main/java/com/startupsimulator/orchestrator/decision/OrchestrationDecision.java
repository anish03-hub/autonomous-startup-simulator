package com.startupsimulator.orchestrator.decision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * CA3 Phase 6A: the structured orchestration decision the LLM returns when the
 * engine asks "given the current startup state, what should happen next?". It is
 * the orchestration-layer analogue of {@code AgentToolStep} — a generic,
 * typed "what to do next" envelope, deliberately NOT one of the department
 * analysis DTOs.
 *
 * <p>The model proposes; Java validates. This record therefore holds the raw,
 * un-coerced proposal: {@code action} and {@code targetAgent} are plain strings
 * exactly as the model produced them. Resolution to the closed
 * {@link OrchestrationAction} vocabulary and to a real
 * {@link com.startupsimulator.model.enums.AgentType} happens only in the
 * {@link OrchestrationDecisionValidator} — nothing here fuzzily converts
 * "developer-ish" into DEVELOPMENT, and an unknown action/agent is surfaced as an
 * explicit rejection, never a silent default.
 *
 * <p>Unknown JSON fields are ignored so a slightly chatty model never crashes the
 * parse; all shape handling (missing action, invalid target, illegal-for-state)
 * is the validator's job.
 *
 * @param action              the proposed action name (resolved via {@link OrchestrationAction#resolve})
 * @param targetAgent         for {@link OrchestrationAction#RUN_AGENT}, the agent to run (an {@code AgentType} name)
 * @param reason              a short, human-readable why
 * @param rationale           the fuller justification the model gives (may cite memory/messages)
 * @param confidence          the model's self-reported confidence 0..1 (optional)
 * @param priority            an optional priority label the model may attach (e.g. HIGH/MEDIUM/LOW)
 * @param requiredInformation optional list of information the model says it still needs
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrchestrationDecision(
        String action,
        String targetAgent,
        String reason,
        String rationale,
        Double confidence,
        String priority,
        List<String> requiredInformation
) {

    public static final String RUN_AGENT = "RUN_AGENT";
    public static final String START_DEBATE = "START_DEBATE";
    public static final String COMPLETE_ANALYSIS = "COMPLETE_ANALYSIS";

    /** The resolved action, or empty when the action string is missing/unknown. */
    public java.util.Optional<OrchestrationAction> resolvedAction() {
        return OrchestrationAction.resolve(action);
    }

    public boolean isRunAgent() {
        return resolvedAction().filter(a -> a == OrchestrationAction.RUN_AGENT).isPresent();
    }

    public boolean isStartDebate() {
        return resolvedAction().filter(a -> a == OrchestrationAction.START_DEBATE).isPresent();
    }

    public boolean isCompleteAnalysis() {
        return resolvedAction().filter(a -> a == OrchestrationAction.COMPLETE_ANALYSIS).isPresent();
    }

    /** True when {@code action} resolves to none of the known actions (incl. null). */
    public boolean isUnknownAction() {
        return resolvedAction().isEmpty();
    }

    public String trimmedTarget() {
        return targetAgent == null ? null : targetAgent.trim();
    }

    public String reasonOrEmpty() {
        return reason == null ? "" : reason.trim();
    }

    public String rationaleOrEmpty() {
        return rationale == null ? "" : rationale.trim();
    }

    public List<String> requiredInformationOrEmpty() {
        return requiredInformation == null ? List.of() : List.copyOf(requiredInformation);
    }

    // ---- Test/convenience factories (production parses via Jackson) ----------

    public static OrchestrationDecision runAgent(String targetAgent, String reason, String rationale) {
        return new OrchestrationDecision(RUN_AGENT, targetAgent, reason, rationale, null, null, null);
    }

    public static OrchestrationDecision startDebate(String reason, String rationale) {
        return new OrchestrationDecision(START_DEBATE, null, reason, rationale, null, null, null);
    }

    public static OrchestrationDecision completeAnalysis(String reason, String rationale) {
        return new OrchestrationDecision(COMPLETE_ANALYSIS, null, reason, rationale, null, null, null);
    }

    /** An explicitly malformed decision (unknown action) for failure-path tests. */
    public static OrchestrationDecision ofRawAction(String rawAction, String targetAgent) {
        return new OrchestrationDecision(rawAction, targetAgent, null, null, null, null, null);
    }
}
