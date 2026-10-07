package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 4B: the structured envelope an agent's LLM returns at <em>each</em> step
 * of a tool-assisted reasoning turn. It is deliberately NOT one of the analysis
 * response DTOs — it is the generic "what do you want to do next" decision the
 * model makes on its own:
 *
 * <ul>
 *   <li>{@code action = "USE_TOOL"} — the model decided it needs a tool. It names
 *       the {@code toolName} (chosen by the model from the catalogue it was shown,
 *       never by Java), a short {@code reason}, and the {@code arguments} map that
 *       must satisfy the tool's schema.</li>
 *   <li>{@code action = "FINAL"} — the model is done and supplies its
 *       {@code finalResponse} prose, produced <em>after</em> having seen any tool
 *       results injected into its prompt.</li>
 * </ul>
 *
 * <p>This is the single semantic distinction Phase 4B turns on. Parsing is the
 * same path every structured response uses ({@code generateStructured(..,
 * AgentToolStep.class)}); unknown JSON fields are ignored so a slightly chatty
 * model never crashes the loop. All malformed-shape handling (missing action,
 * USE_TOOL without a tool name, unknown action) is the reasoner's job — this
 * record only exposes typed, null-safe accessors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentToolStep(
        String action,
        String toolName,
        String reason,
        Map<String, Object> arguments,
        String finalResponse
) {

    public static final String USE_TOOL = "USE_TOOL";
    public static final String FINAL = "FINAL";

    /** True when the model asked to run a tool (case-insensitive, trimmed). */
    public boolean isUseTool() {
        return USE_TOOL.equalsIgnoreCase(trimmed(action));
    }

    /** True when the model declared it is finished and supplied a final answer. */
    public boolean isFinal() {
        return FINAL.equalsIgnoreCase(trimmed(action));
    }

    /** True when {@code action} is neither USE_TOOL nor FINAL (incl. null). */
    public boolean isUnknownAction() {
        return !isUseTool() && !isFinal();
    }

    public String trimmedToolName() {
        return trimmed(toolName);
    }

    public Map<String, Object> argumentsOrEmpty() {
        return arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
    }

    public String finalResponseOrEmpty() {
        return finalResponse == null ? "" : finalResponse.trim();
    }

    // ---- Test/convenience factories (production parses via Jackson) ----------

    public static AgentToolStep useTool(String toolName, String reason, Map<String, Object> arguments) {
        return new AgentToolStep(USE_TOOL, toolName, reason, arguments, null);
    }

    public static AgentToolStep finalAnswer(String finalResponse) {
        return new AgentToolStep(FINAL, null, null, null, finalResponse);
    }

    private static String trimmed(String s) {
        return s == null ? null : s.trim();
    }
}
