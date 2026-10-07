package com.startupsimulator.tool;

import java.util.List;

/**
 * Structured, serialisable description of a {@link Tool}: its unique name, a
 * human-/LLM-readable description, and the typed input parameters it accepts.
 *
 * <p>This is the "tool metadata for future LLM use" artefact (Phase 4A §8). The
 * {@link ToolRegistry} exposes a list of these so Phase 4B can hand the LLM a
 * catalogue of callable tools and their input schema — WITHOUT this layer
 * knowing anything about the LLM. No function-calling mechanism is built here.
 */
public record ToolSchema(
        String toolName,
        String description,
        List<ToolParameter> parameters
) {
    public ToolSchema {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
