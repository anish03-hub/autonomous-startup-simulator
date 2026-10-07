package com.startupsimulator.agent;

import java.util.List;

/**
 * Phase 4B: the result of one tool-assisted reasoning invocation
 * ({@link AgentToolReasoner#reason}). It records what the LLM ultimately said and
 * everything the agent did on the way there, without ever having let Java pick a
 * tool or write the final answer.
 *
 * @param usedRealLlm       whether a genuine LLM path ran (REAL mode + real
 *                          provider). When false, no tool selection or final
 *                          answer is fabricated — see {@link #noRealProvider()}.
 * @param failed            true when the LLM/transport/parse failed; the existing
 *                          REAL-mode explicit-failure semantics are preserved
 *                          (nothing fabricated).
 * @param finalResponse     the model's own final prose (only populated on a FINAL
 *                          termination), produced after it saw any tool results.
 * @param interactions      ordered record of every tool request/result this
 *                          invocation (section F/G), each individually addressable.
 * @param terminationReason why the loop ended (see the constants).
 */
public record ToolAssistedOutcome(
        boolean usedRealLlm,
        boolean failed,
        String finalResponse,
        List<ToolInteraction> interactions,
        String terminationReason
) {

    /** The model returned a FINAL step: {@link #finalResponse()} is authoritative. */
    public static final String FINAL = "FINAL";
    /** The tool-call budget was exhausted before the model returned FINAL. */
    public static final String BUDGET_EXHAUSTED = "BUDGET_EXHAUSTED";
    /** The LLM/transport/parse failed (REAL-mode explicit failure, no fabrication). */
    public static final String LLM_FAILURE = "LLM_FAILURE";
    /** No real LLM was available, so no genuine tool selection could occur. */
    public static final String NO_REAL_PROVIDER = "NO_REAL_PROVIDER";

    public ToolAssistedOutcome {
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
    }

    /** Number of tools that actually executed (malformed steps excluded). */
    public int toolExecutions() {
        return (int) interactions.stream().filter(i -> !i.isMalformed()).count();
    }

    /** True when at least one real tool execution occurred this invocation. */
    public boolean usedTool() {
        return toolExecutions() > 0;
    }

    static ToolAssistedOutcome finalized(String finalResponse, List<ToolInteraction> interactions) {
        return new ToolAssistedOutcome(true, false, finalResponse, interactions, FINAL);
    }

    static ToolAssistedOutcome budgetExhausted(List<ToolInteraction> interactions) {
        return new ToolAssistedOutcome(true, false, null, interactions, BUDGET_EXHAUSTED);
    }

    static ToolAssistedOutcome llmFailure(List<ToolInteraction> interactions) {
        return new ToolAssistedOutcome(true, true, null, interactions, LLM_FAILURE);
    }

    static ToolAssistedOutcome noRealProvider() {
        return new ToolAssistedOutcome(false, false, null, List.of(), NO_REAL_PROVIDER);
    }
}
