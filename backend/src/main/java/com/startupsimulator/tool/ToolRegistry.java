package com.startupsimulator.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The catalogue of available tools. It is populated at startup from every
 * {@link Tool} Spring finds (constructor-injected {@code List<Tool>}), rejecting
 * duplicate names loudly, and thereafter answers lookups and exposes
 * {@link ToolSchema} metadata for the eventual Phase 4B LLM layer.
 *
 * <p>The registry is intentionally independent of agents: it knows nothing about
 * who calls a tool or why. For unit tests it can also be built from
 * {@code List.of()} and populated via {@link #register(Tool)}.
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> discoveredTools) {
        if (discoveredTools != null) {
            for (Tool tool : discoveredTools) {
                register(tool);
            }
        }
    }

    /**
     * Register a tool. Fails clearly on a duplicate name — never silently
     * replaces an existing registration.
     */
    public void register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Cannot register a null tool.");
        }
        String name = tool.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must not be blank.");
        }
        if (tools.containsKey(name)) {
            throw new IllegalStateException(
                    "A tool named '" + name + "' is already registered; duplicate registration refused.");
        }
        tools.put(name, tool);
    }

    /** Resolve a tool by its unique name. */
    public Optional<Tool> find(String name) {
        return Optional.ofNullable(name == null ? null : tools.get(name));
    }

    public boolean contains(String name) {
        return name != null && tools.containsKey(name);
    }

    public int size() {
        return tools.size();
    }

    /** Structured, serialisable metadata for every registered tool (Phase 4B catalogue). */
    public List<ToolSchema> listAvailable() {
        List<ToolSchema> schemas = new ArrayList<>(tools.size());
        for (Tool tool : tools.values()) {
            schemas.add(tool.schema());
        }
        return List.copyOf(schemas);
    }
}
