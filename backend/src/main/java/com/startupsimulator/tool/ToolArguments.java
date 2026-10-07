package com.startupsimulator.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A typed, validated view over a tool's raw input arguments.
 *
 * <p>This is the single, deliberately-contained boundary where loosely-typed
 * input (the kind a Phase 4B LLM will emit as JSON) is turned into checked Java
 * values. Rather than scattering {@code Map<String,Object>} casts across every
 * tool, callers use the typed accessors here, each of which raises a controlled
 * {@link ToolValidationException} (never an uncaught {@link ClassCastException}
 * or {@link NullPointerException}) with a machine-readable code:
 * {@code MISSING_ARGUMENT}, {@code INVALID_TYPE}, {@code INVALID_NUMBER}.
 *
 * <p>Range checks are the tool's responsibility (it knows its own domain) and
 * use {@code OUT_OF_RANGE}; see {@link AbstractTool#inRange}.
 */
public final class ToolArguments {

    private final Map<String, Object> raw;

    private ToolArguments(Map<String, Object> raw) {
        this.raw = raw;
    }

    public static ToolArguments of(Map<String, Object> values) {
        return new ToolArguments(values == null ? Map.of() : new LinkedHashMap<>(values));
    }

    public static ToolArguments empty() {
        return new ToolArguments(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean has(String key) {
        return raw.get(key) != null;
    }

    /** The underlying key set — used only for metadata/diagnostics, never for execution logic. */
    public java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(raw.keySet());
    }

    // ---- Typed, validated accessors -----------------------------------------

    public double requireDouble(String key) {
        return finite(key, number(key).doubleValue());
    }

    public double optionalDouble(String key, double defaultValue) {
        return has(key) ? requireDouble(key) : defaultValue;
    }

    public int requireInt(String key) {
        Number n = number(key);
        double d = finite(key, n.doubleValue());
        if (d != Math.rint(d)) {
            throw new ToolValidationException("INVALID_TYPE",
                    "Argument '" + key + "' must be a whole number.");
        }
        return (int) d;
    }

    public int optionalInt(String key, int defaultValue) {
        return has(key) ? requireInt(key) : defaultValue;
    }

    public List<Integer> requireIntList(String key) {
        Object v = require(key);
        if (!(v instanceof List<?> list)) {
            throw new ToolValidationException("INVALID_TYPE",
                    "Argument '" + key + "' must be an array of whole numbers.");
        }
        List<Integer> out = new java.util.ArrayList<>(list.size());
        for (Object e : list) {
            if (!(e instanceof Number n)) {
                throw new ToolValidationException("INVALID_TYPE",
                        "Argument '" + key + "' must contain only numbers.");
            }
            double d = finite(key, n.doubleValue());
            if (d != Math.rint(d)) {
                throw new ToolValidationException("INVALID_TYPE",
                        "Argument '" + key + "' must contain only whole numbers.");
            }
            out.add((int) d);
        }
        return List.copyOf(out);
    }

    // ---- internals ----------------------------------------------------------

    private Object require(String key) {
        Object v = raw.get(key);
        if (v == null) {
            throw new ToolValidationException("MISSING_ARGUMENT",
                    "Missing required argument: '" + key + "'.");
        }
        return v;
    }

    private Number number(String key) {
        Object v = require(key);
        if (!(v instanceof Number n)) {
            throw new ToolValidationException("INVALID_TYPE",
                    "Argument '" + key + "' must be a number.");
        }
        return n;
    }

    private static double finite(String key, double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new ToolValidationException("INVALID_NUMBER",
                    "Argument '" + key + "' must be a finite number.");
        }
        return d;
    }

    /** Small fluent builder so tests and (later) callers avoid raw maps. */
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        public Builder put(String key, Object value) {
            values.put(key, value);
            return this;
        }

        public ToolArguments build() {
            return new ToolArguments(values);
        }
    }
}
