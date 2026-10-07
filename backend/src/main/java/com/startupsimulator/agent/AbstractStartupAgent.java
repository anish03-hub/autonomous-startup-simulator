package com.startupsimulator.agent;

import com.startupsimulator.model.enums.AgentType;

/** Shared plumbing for concrete agents: LLM access and a phrasing helper. */
public abstract class AbstractStartupAgent implements StartupAgent {

    protected final LLMService llm;

    protected AbstractStartupAgent(LLMService llm) {
        this.llm = llm;
    }

    /**
     * Route a composed draft through the LLM seam. The system prompt frames the
     * department persona; the draft carries the facts. In mock mode the draft is
     * returned as-is.
     */
    protected String say(String draft) {
        String system = "You are the " + type().getDisplayName()
                + " agent of an early-stage startup. Mandate: " + type().getMandate()
                + ". Speak concisely and decisively in the first person.";
        return llm.complete(system, draft);
    }

    public abstract AgentType type();
}
