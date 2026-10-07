package com.startupsimulator.model.enums;

/** The four founding AI departments of a virtual startup. */
public enum AgentType {
    CEO("Chief Executive", "Strategy, vision, target customer, business model and final decisions"),
    DEVELOPMENT("Development", "MVP scope, technical feasibility, architecture, stack and estimates"),
    MARKETING("Marketing", "Audience, competitors, positioning, pricing and go-to-market"),
    FINANCE("Finance", "Costs, budget, revenue assumptions, burn rate, runway and financial risk");

    private final String displayName;
    private final String mandate;

    AgentType(String displayName, String mandate) {
        this.displayName = displayName;
        this.mandate = mandate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMandate() {
        return mandate;
    }
}
