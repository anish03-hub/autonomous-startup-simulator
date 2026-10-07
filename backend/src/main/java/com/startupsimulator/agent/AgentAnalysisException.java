package com.startupsimulator.agent;

/**
 * Thrown when a department agent's real (LLM-backed) analysis cannot be used:
 * a transport/provider error, a parse failure, or a response that fails
 * structural validation. The agent catches this and falls back to its
 * deterministic analysis so the simulation always completes — mirroring
 * {@link CeoAnalysisException} for the department agents.
 */
public class AgentAnalysisException extends RuntimeException {

    public AgentAnalysisException(String message) {
        super(message);
    }

    public AgentAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
