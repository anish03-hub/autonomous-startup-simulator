package com.startupsimulator.agent;

import com.startupsimulator.model.*;
import com.startupsimulator.model.enums.AgentType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The shared, authoritative working state of a startup that every agent reads
 * from and writes to during a simulation. It wraps the managed {@link Startup}
 * aggregate plus the working collections produced by the departments. The
 * orchestrator persists these collections; the {@link Startup}'s own fields are
 * mutated in place and saved.
 *
 * <p>This is the concrete realisation of the "StartupContext" concept from the
 * brief: the backend owns it, never the frontend.
 */
@Getter
public class StartupContext {

    private final Startup startup;
    private final List<MvpFeature> mvpFeatures = new ArrayList<>();
    private final List<RoadmapMilestone> roadmap = new ArrayList<>();
    private final List<String> risks = new ArrayList<>();
    private final TechnicalPlan technicalPlan;
    private final MarketingPlan marketingPlan;
    private final Budget budget;

    /**
     * Phase 3 messaging seam. The orchestrator delivers into these collections
     * (reading the persisted inbox) before an agent runs, and drains the queued
     * outgoing intents afterwards to dispatch them. The agents themselves never
     * touch the repository — they read {@link #inboxFor}/{@link #sentFor} when
     * building prompts and {@link #queueOutgoing} to express a send intent. This
     * keeps the agent constructors (and all Phase-2 tests) unchanged.
     */
    private final Map<AgentType, List<AgentMessage>> inbox = new EnumMap<>(AgentType.class);
    private final Map<AgentType, List<AgentMessage>> sent = new EnumMap<>(AgentType.class);
    private final List<OutgoingMessage> outgoing = new ArrayList<>();

    /** A send intent queued by {@code sender}, awaiting validation + delivery. */
    public record OutgoingMessage(AgentType sender, AgentMessageIntent intent) {}

    /**
     * Phase 5B memory seam, mirroring the Phase 3 messaging seam but kept
     * <em>distinct</em> from it (a message and a persistent memory are different
     * concepts and must never be collapsed): the memory-aware reasoning path sets
     * {@link #relevantMemory} to a pre-rendered, bounded memory block before an
     * agent runs, and queues the agent's memory-creation intents afterwards for the
     * caller to persist through {@code MemoryService} once the analysis succeeds.
     * The agents themselves never touch the memory repository.
     */
    private String relevantMemory;
    private final List<MemoryIntentCandidate> memoryIntents = new ArrayList<>();

    /** A memory-creation intent queued by {@code author}, awaiting validated persistence. */
    public record MemoryIntentCandidate(AgentType author, AgentMemoryIntent intent) {}

    public StartupContext(Startup startup) {
        this.startup = startup;
        this.technicalPlan = new TechnicalPlan(startup.getId());
        this.marketingPlan = new MarketingPlan(startup.getId());
        this.budget = new Budget(startup.getId());
    }

    public Long startupId() {
        return startup.getId();
    }

    public String idea() {
        return startup.getOriginalIdea();
    }

    public String startupName() {
        return startup.getName();
    }

    public void addFeature(MvpFeature feature) {
        feature.setDisplayOrder(mvpFeatures.size());
        mvpFeatures.add(feature);
    }

    public void addRisk(String risk) {
        if (risk != null && !risk.isBlank()) {
            risks.add(risk.trim());
        }
    }

    public void addMilestone(RoadmapMilestone milestone) {
        roadmap.add(milestone);
    }

    public long mvpFeatureCount() {
        return mvpFeatures.stream().filter(MvpFeature::isInMvp).count();
    }

    // ---- Phase 3: agent-to-agent messaging ----------------------------------

    /** Deliver the messages addressed to {@code recipient} (section C of its prompt). */
    public void deliverInbox(AgentType recipient, List<AgentMessage> messages) {
        inbox.put(recipient, messages == null ? List.of() : List.copyOf(messages));
    }

    /** Messages addressed TO {@code recipient} that were delivered this run. */
    public List<AgentMessage> inboxFor(AgentType recipient) {
        return inbox.getOrDefault(recipient, List.of());
    }

    /** Record the messages {@code sender} has sent so far (section D of its prompt). */
    public void recordSent(AgentType sender, List<AgentMessage> messages) {
        sent.put(sender, messages == null ? List.of() : List.copyOf(messages));
    }

    /** Messages previously sent BY {@code sender}. */
    public List<AgentMessage> sentFor(AgentType sender) {
        return sent.getOrDefault(sender, List.of());
    }

    /** Queue a send intent expressed by {@code sender} during its analysis turn. */
    public void queueOutgoing(AgentType sender, AgentMessageIntent intent) {
        if (sender != null && intent != null && intent.isPresent()) {
            outgoing.add(new OutgoingMessage(sender, intent));
        }
    }

    /** Return and clear the queued outgoing intents (the orchestrator dispatches them). */
    public List<OutgoingMessage> drainOutgoing() {
        List<OutgoingMessage> drained = List.copyOf(outgoing);
        outgoing.clear();
        return drained;
    }

    // ---- Phase 5B: persistent-memory reasoning seam -------------------------

    /** Set the pre-rendered, bounded memory block the memory-aware path injects (section E). */
    public void setRelevantMemory(String block) {
        this.relevantMemory = block;
    }

    /**
     * The rendered memory block for this turn, or "" when the plain (non
     * memory-aware) path ran — so existing prompt assertions are unaffected and
     * no empty section is appended on the Phase 2/3/4 paths.
     */
    public String relevantMemoryBlock() {
        return relevantMemory == null ? "" : relevantMemory;
    }

    /** Queue the memory-creation intents {@code author} expressed during its analysis turn. */
    public void queueMemoryIntents(AgentType author, List<AgentMemoryIntent> intents) {
        if (author == null || intents == null) {
            return;
        }
        for (AgentMemoryIntent intent : intents) {
            if (intent != null && intent.isPresent()) {
                memoryIntents.add(new MemoryIntentCandidate(author, intent));
            }
        }
    }

    /** Return and clear the queued memory-creation intents (the caller persists them). */
    public List<MemoryIntentCandidate> drainMemoryIntents() {
        List<MemoryIntentCandidate> drained = List.copyOf(memoryIntents);
        memoryIntents.clear();
        return drained;
    }
}
