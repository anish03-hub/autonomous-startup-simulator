package com.startupsimulator.agent;

import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.MvpFeature;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.TechnicalPlan;
import com.startupsimulator.model.enums.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Development department: MVP scope, feasibility, architecture, stack, timeline
 * and technical risk.
 *
 * <p>Phase 2B: the analysis is produced by a real LLM through the
 * {@link LLMService} seam into a typed {@link DeveloperAnalysisResponse}, built
 * on the CEO's strategic analysis (injected via {@link #ceoContextBlock}). In
 * SCRIPTED_DEMO mode it uses the deterministic Phase 1 analysis instead, which
 * also guarantees the scripted boardroom's debate anchors ("Community feed" =
 * heavy/non-core, "Product scanner" = core). In REAL mode the model's own
 * feature set is authoritative and those anchors are never injected.
 */
@Component
public class DeveloperAgent extends DepartmentAgent {

    public DeveloperAgent(LLMService llm, LlmProperties llmProperties) {
        super(llm, llmProperties, "prompts/developer-system-prompt.txt");
    }

    @Override
    public AgentType type() {
        return AgentType.DEVELOPMENT;
    }

    @Override
    public String initialObjective(StartupContext ctx) {
        return "Define the MVP scope and technical architecture.";
    }

    @Override
    public List<String> plannedSubtasks() {
        return List.of("Feature analysis", "Technical feasibility", "Technology selection",
                "Architecture", "Development estimate");
    }

    // ---- Real (LLM) path ----------------------------------------------------

    @Override
    protected String applyRealAnalysis(StartupContext ctx) {
        DeveloperAnalysisResponse raw =
                llm.generateStructured(systemPrompt, buildUserPrompt(ctx), DeveloperAnalysisResponse.class);
        if (raw == null || !raw.isValid()) {
            throw new AgentAnalysisException("Development analysis response failed validation.");
        }
        DeveloperAnalysisResponse r = raw.normalized();

        for (DeveloperAnalysisResponse.FeatureProposal f : r.mvpFeatures()) {
            ctx.addFeature(new MvpFeature(ctx.startupId(), f.name(), f.description(),
                    Boolean.TRUE.equals(f.inMvp()), f.effort() == null ? 3 : f.effort()));
        }
        // REAL mode: the LLM's feature set is authoritative. We deliberately do
        // NOT inject the scripted "Product scanner" / "Community feed" debate
        // anchors here — doing so would overwrite genuine model output. The
        // boardroom debate derives its scope decision from whatever the model
        // actually proposed (see BoardroomDebate).

        TechnicalPlan tp = ctx.getTechnicalPlan();
        tp.setArchitecture(r.architecture());
        tp.setTechStack(joinCsv(r.techStack()));
        tp.setTimeline(r.timeline());
        tp.setTechnicalRisks(joinSentences(r.technicalRisks()));
        tp.setEstimatedEngineeringMonths(r.estimatedEngineeringMonths());

        Startup s = ctx.getStartup();
        s.setTechnicalFeasibility(r.technicalFeasibility());
        s.setSolution(r.solutionSummary());

        r.technicalRisks().forEach(ctx::addRisk);

        // Phase 3: if the model chose to address another department (e.g. Finance),
        // queue that intent. The orchestrator validates the recipient and persists
        // it as a real AgentMessage after this turn — the agent never writes the row.
        ctx.queueOutgoing(type(), r.messageIntent());

        // Phase 5B: queue any agent-driven memory-creation intents. These are
        // persisted (bounded, validated, de-duplicated) only after this real
        // analysis succeeds, by the memory-aware entry point — never here, and
        // never on the deterministic path. Generic for every department.
        ctx.queueMemoryIntents(type(), r.memoryIntents());

        return "MVP scoped: " + r.solutionSummary() + " (~"
                + r.estimatedEngineeringMonths() + " engineering months). I recommend deferring the "
                + "heaviest non-core feature to V2 to protect the timeline.";
    }

    // ---- Deterministic (Phase 1) path & fallback ----------------------------

    @Override
    protected String applyDeterministicAnalysis(StartupContext ctx) {
        boolean ai = IdeaAnalyzer.isAiPowered(ctx.idea());
        String noun = IdeaAnalyzer.productNoun(ctx.idea());

        ctx.addFeature(new MvpFeature(ctx.startupId(), "Guided onboarding",
                "Fast first-run flow that captures the inputs the " + noun + " needs.", true, 2));
        ctx.addFeature(new MvpFeature(ctx.startupId(),
                ai ? "AI recommendation engine" : "Core workflow engine",
                ai ? "The core model/heuristics that turn user inputs into personalised results."
                   : "The core logic that delivers the product's primary value.", true, 4));
        ctx.addFeature(new MvpFeature(ctx.startupId(), "User profiles & history",
                "Accounts, saved results and a lightweight history view.", true, 2));
        ensureDebateAnchors(ctx);

        TechnicalPlan tp = ctx.getTechnicalPlan();
        tp.setArchitecture("Modular monolith: React SPA + Spring Boot API + PostgreSQL, "
                + (ai ? "with an isolated inference service behind an interface for the model." : "with a clean service layer."));
        tp.setTechStack(ai
                ? "React, TypeScript, Spring Boot, PostgreSQL, Redis (cache), Python inference service, S3-compatible storage"
                : "React, TypeScript, Spring Boot, PostgreSQL, Redis (cache), S3-compatible storage");
        tp.setTimeline("Phase 1 (Weeks 1-6): core engine + onboarding. Phase 2 (Weeks 7-12): scanner + profiles + polish.");
        tp.setTechnicalRisks("Model quality/latency depends on data" + (ai ? "" : " and integrations")
                + "; scanner accuracy needs real-world testing; scope creep from social features.");
        tp.setEstimatedEngineeringMonths(5.0);

        Startup s = ctx.getStartup();
        s.setTechnicalFeasibility(78);
        s.setSolution("A " + (ai ? "AI-powered " : "") + noun + " that " + coreBenefit(ctx) + ".");

        ctx.addRisk("Engineering timeline risk: full scope (incl. community) trends toward ~5 months.");
        return say("At full scope the build is roughly 5 months. I recommend postponing the Community feed to V2 — "
                + "it adds significant complexity without being necessary for the core MVP.");
    }

    @Override
    protected void recordAnalysisMeta(StartupContext ctx, String provider, boolean failed, String error) {
        TechnicalPlan tp = ctx.getTechnicalPlan();
        tp.setAnalysisProvider(provider);
        tp.setAnalysisFailed(failed);
        tp.setAnalysisError(error);
    }

    @Override
    public String debateStatement(StartupContext ctx) {
        return say("The current feature scope requires approximately 5 months. If we want to launch sooner, "
                + "the Community feed is the clearest cut — the core engine and scanner are not.");
    }

    /**
     * Guarantee the two features the scripted boardroom debate depends on exist,
     * regardless of what the LLM proposed: the "Community feed" (deliberately
     * heavy and non-core, which the CEO trims to V2) and the "Product scanner"
     * (core, which Marketing defends). Phase 2C will make the debate derive its
     * anchors dynamically; until then this keeps the decision/blueprint stable.
     */
    private void ensureDebateAnchors(StartupContext ctx) {
        boolean hasScanner = ctx.getMvpFeatures().stream()
                .anyMatch(f -> f.getName() != null && f.getName().toLowerCase().contains("scanner"));
        if (!hasScanner) {
            ctx.addFeature(new MvpFeature(ctx.startupId(), "Product scanner",
                    "Camera/upload capture that feeds the recommendation flow — key to positioning.", true, 3));
        }
        boolean hasCommunity = ctx.getMvpFeatures().stream()
                .anyMatch(f -> f.getName() != null && f.getName().equalsIgnoreCase("Community feed"));
        if (!hasCommunity) {
            // Deliberately heavy, non-essential feature — the debate will trim this.
            ctx.addFeature(new MvpFeature(ctx.startupId(), "Community feed",
                    "Social feed where users share results and discuss. High effort, not core to V1.", true, 5));
        }
    }

    private String coreBenefit(StartupContext ctx) {
        String domain = IdeaAnalyzer.domain(ctx.idea());
        return "gives " + IdeaAnalyzer.audience(ctx.idea()) + " fast, personalised results in " + domain;
    }

    /** Compose the user prompt: the CEO's analysis first, then the idea to build on. */
    String buildUserPrompt(StartupContext ctx) {
        Startup s = ctx.getStartup();
        StringBuilder sb = new StringBuilder();
        sb.append(ceoContextBlock(ctx)).append('\n');
        sb.append(communicationBlock(ctx, type())).append('\n');
        String memory = memoryBlock(ctx);
        if (!memory.isEmpty()) {
            sb.append(memory).append('\n');
        }
        sb.append("Startup name: ").append(s.getName()).append('\n');
        sb.append("Founder's idea: ").append(IdeaAnalyzer.oneLine(ctx.idea())).append('\n');
        sb.append("\nProduce the Development analysis as JSON per the required format. "
                + "Keep the MVP focused and call out any heavy, non-essential feature as deferrable.");
        return sb.toString();
    }
}
