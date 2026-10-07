package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Phase 3: a structured communication intent produced by an agent's LLM
 * <em>within</em> its existing structured response — NOT a tool/function call.
 * The agent chooses a {@code recipient} (by {@link com.startupsimulator.model.enums.AgentType}
 * name or display name), an optional {@code subject}, and the {@code message} body.
 *
 * <p>The agent only expresses the intent; the messaging infrastructure
 * ({@code AgentInbox}) is solely responsible for validating the recipient and
 * delivering (persisting) the message. An absent or incomplete intent means the
 * agent chose not to address anyone this turn.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentMessageIntent(String recipient, String subject, String message) {

    /** True only when the agent actually named a recipient and wrote a body. */
    public boolean isPresent() {
        return notBlank(recipient) && notBlank(message);
    }

    public String normalizedRecipient() {
        return recipient == null ? null : recipient.trim();
    }

    public String normalizedSubject() {
        return notBlank(subject) ? subject.trim() : null;
    }

    public String normalizedMessage() {
        return message == null ? null : message.trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
