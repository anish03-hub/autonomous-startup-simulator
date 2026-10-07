package com.startupsimulator.tool;

/**
 * The declared type of a single tool input parameter. Deliberately small and
 * machine-readable so Phase 4B can map it onto an LLM function/tool JSON schema
 * without changing this layer.
 */
public enum ToolParamType {
    NUMBER,
    INTEGER,
    STRING,
    BOOLEAN,
    INTEGER_ARRAY
}
