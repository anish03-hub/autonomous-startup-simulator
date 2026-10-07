package com.startupsimulator.orchestrator.replan;

/**
 * CA3 Phase 7: Result of server-side validation of a {@link ReplanProposal}.
 */
public record ReplanValidationOutcome(
        boolean valid,
        String reason,
        ReplanProposal proposal
) {
    public static ReplanValidationOutcome accepted(ReplanProposal proposal) {
        return new ReplanValidationOutcome(true, "Proposal accepted by server validator.", proposal);
    }

    public static ReplanValidationOutcome rejected(String reason, ReplanProposal proposal) {
        return new ReplanValidationOutcome(false, reason, proposal);
    }
}
