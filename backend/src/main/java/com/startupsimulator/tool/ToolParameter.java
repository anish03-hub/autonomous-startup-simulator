package com.startupsimulator.tool;

/**
 * One declared input parameter of a {@link Tool}. This is pure metadata — it
 * describes the parameter so a future LLM tool-selection layer (Phase 4B) can
 * build a JSON schema and so the execution layer can document what it expects.
 * It carries optional inclusive numeric bounds ({@code min}/{@code max}); a null
 * bound means "unbounded on that side".
 *
 * @param name        machine-readable argument key (as it appears in {@link ToolArguments})
 * @param type        the declared {@link ToolParamType}
 * @param required    whether the argument must be present
 * @param description human-/LLM-readable explanation of the parameter
 * @param min         inclusive lower bound for numeric params, or null
 * @param max         inclusive upper bound for numeric params, or null
 */
public record ToolParameter(
        String name,
        ToolParamType type,
        boolean required,
        String description,
        Double min,
        Double max
) {
    /** A required parameter with no numeric bounds. */
    public static ToolParameter required(String name, ToolParamType type, String description) {
        return new ToolParameter(name, type, true, description, null, null);
    }

    /** An optional parameter with no numeric bounds. */
    public static ToolParameter optional(String name, ToolParamType type, String description) {
        return new ToolParameter(name, type, false, description, null, null);
    }

    /** A required numeric parameter constrained to the inclusive range [min, max]. */
    public static ToolParameter requiredRange(String name, ToolParamType type, String description,
                                              Double min, Double max) {
        return new ToolParameter(name, type, true, description, min, max);
    }

    /** An optional numeric parameter constrained to the inclusive range [min, max]. */
    public static ToolParameter optionalRange(String name, ToolParamType type, String description,
                                              Double min, Double max) {
        return new ToolParameter(name, type, false, description, min, max);
    }
}
