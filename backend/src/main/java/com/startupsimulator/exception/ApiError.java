package com.startupsimulator.exception;

import java.time.Instant;
import java.util.List;

/** Standard error body returned by the API. */
public record ApiError(Instant timestamp, int status, String error, String message, List<String> details) {
    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
