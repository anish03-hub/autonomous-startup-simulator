package com.startupsimulator.dto.response;

import com.startupsimulator.trace.ExecutionTraceEntry;
import com.startupsimulator.trace.ExecutionTraceType;

import java.time.Instant;

/**
 * Read DTO representing an execution trace record returned by GET /api/startups/{id}/trace.
 */
public record ExecutionTraceEntryDto(
        Long id,
        Long startupId,
        String traceId,
        int sequenceNumber,
        Instant timestamp,
        ExecutionTraceType type,
        String agent,
        String sender,
        String recipient,
        String action,
        String decision,
        String reason,
        String toolName,
        String toolArguments,
        String toolResult,
        String outcome,
        String payload
) {
    public static ExecutionTraceEntryDto from(ExecutionTraceEntry entry) {
        if (entry == null) return null;
        return new ExecutionTraceEntryDto(
                entry.getId(),
                entry.getStartupId(),
                entry.getTraceId(),
                entry.getSequenceNumber(),
                entry.getTimestamp(),
                entry.getType(),
                entry.getAgent(),
                entry.getSender(),
                entry.getRecipient(),
                entry.getAction(),
                entry.getDecision(),
                entry.getReason(),
                entry.getToolName(),
                entry.getToolArguments(),
                entry.getToolResult(),
                entry.getOutcome(),
                entry.getPayload()
        );
    }
}
