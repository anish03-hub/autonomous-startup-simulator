package com.startupsimulator.memory;

/**
 * Phase 5A: a controlled, coded validation failure in the memory subsystem — the
 * same explicit-failure style as {@code ToolValidationException}. {@link MemoryService}
 * throws this for an invalid {@link CreateMemoryRequest}; it never persists a
 * malformed row and never fabricates one.
 */
public class MemoryValidationException extends RuntimeException {

    private final String code;

    public MemoryValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
