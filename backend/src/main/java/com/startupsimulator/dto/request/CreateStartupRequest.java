package com.startupsimulator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating a new startup from a raw idea. */
public record CreateStartupRequest(
        @NotBlank(message = "idea is required")
        @Size(min = 8, max = 2000, message = "idea must be between 8 and 2000 characters")
        String idea,

        @Size(max = 60, message = "name must be at most 60 characters")
        String name
) {
}
