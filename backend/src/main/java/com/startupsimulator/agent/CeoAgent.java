package com.startupsimulator.agent;

import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.RoadmapMilestone;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CEO: overall strategy, vision, target customer, business model, priorities
 * and — crucially — resolving disagreements between departments.
 *
 * <p>Phase 2A: the CEO's <em>analysis</em> is produced by a real LLM through the
 * {@link LLMService} seam ({@link LLMService#generateStructured}) into a typed
 * {@link CeoAnalysisResponse}, then validated and written to the authoritative
 * {@link Startup} state. When no real provider is configured (the default
 * offline/mock mode) — or when a real call fails — the CEO falls back to a
 * deterministic analysis so the simulation always completes, exactly as in
 * Phase 1. The debate/decision phase remains scripted (Phase 2C will make it
 * dynamic).
 */
@Component
public class CeoAgent extends AbstractStartupAgent {

    private static final Logger log = LoggerFactory.getLogger(CeoAgent.class);
    private static final String SYSTEM_PROMPT_PATH = "prompts/ceo-system-prompt.txt";

    private final LlmProperties llmProperties;
    private final String systemPrompt;

    public CeoAgent(LLMService llm, LlmProperties llmProperties) {
        super(llm);
        this.llmProperties = llmProperties;
        this.systemPrompt = loadSystemPrompt();
    }

    @Override
    public AgentType type() {
        return AgentType.CEO;
    }

    /** Whether the CEO will attempt a real LLM call (a provider with credentials). */
    public boolean usesRealProvider() {
        return llm.isRealProvider();
    }

    /**
     * Whether a genuine LLM reasoning path will actually run: REAL mode requested
     * <em>and</em> a real provider available. This is the authoritative-output
     * gate (as opposed to {@link #usesRealProvider()}).
     */
    public boolean usesRealLlm() {
        return llmProperties.isRealMode() && llm.isRealProvider();
    }

    /** Active provider identifier, e.g. "openai" or "mock". */
    public String providerName() {
        return llm.provider();
    }

    @Override
    public String initialObjective(StartupContext ctx) {
        return "Set the strategy, target customer and business model.";
    }

    @Override
    public List<String> plannedSubtasks() {
        return List.of("Vision & problem", "Target customer", "Business model",
                "Priorities", "Strategic direction");
    }

    /**
     * Run the CEO's strategic analysis. Uses the real LLM when a provider is
     * configured, otherwise a deterministic analysis. Never throws: on a real
     * call failure it records the failure on the startup and falls back, so the
     * orchestrator can continue. Returns the outcome (message + provider +
     * failure info) for event emission by the caller.
     */
    @Override
    public String analyze(StartupContext ctx) {
        return runAnalysis(ctx).headlineMessage();
    }

    /**
     * The full analysis with metadata, used by the orchestrator to emit the
     * correct CEO/LLM events and by the retry endpoint.
     */
    public CeoAnalysisOutcome runAnalysis(StartupContext ctx) {
        Startup s = ctx.getStartup();

        if (llmProperties.isRealMode()) {
            if (!llm.isRealProvider()) {
                // REAL requested but no real provider — explicit failure, no fabrication.
                String msg = "REAL mode requires a configured LLM provider, but '" + llm.provider()
                        + "' cannot reach a model. No CEO analysis was produced.";
                log.warn("CEO analysis skipped: {}", msg);
                s.setCeoAnalysisProvider("unavailable");
                s.setCeoAnalysisFailed(true);
                s.setCeoAnalysisError(msg);
                return CeoAnalysisOutcome.failed(null, "unavailable", msg,
                        "CEO analysis skipped: no real LLM provider is available in REAL mode.");
            }
            try {
                log.info("CEO analysis starting via real provider '{}' for startup {}", llm.provider(), ctx.startupId());
                CeoAnalysisResponse response = callLlm(ctx);
                applyToStartup(ctx, response, llm.provider(), true);
                applyRoadmap(ctx, response, true);
                s.setCeoAnalysisFailed(false);
                s.setCeoAnalysisError(null);
                log.info("CEO analysis completed via '{}' for startup {}", llm.provider(), ctx.startupId());
                return CeoAnalysisOutcome.success(response, llm.provider(),
                        response.executiveSummary());
            } catch (CeoAnalysisException e) {
                // REAL mode: invalid/failed output is an explicit failure — we do NOT
                // write a fabricated deterministic analysis over it.
                String err = shortError(e);
                log.warn("CEO REAL analysis failed for startup {} ({}); recording explicit failure (no fabrication).",
                        ctx.startupId(), err);
                s.setCeoAnalysisProvider("failed");
                s.setCeoAnalysisFailed(true);
                s.setCeoAnalysisError(err);
                return CeoAnalysisOutcome.failed(null, "failed", err, "CEO analysis failed: " + err);
            }
        }

        // SCRIPTED_DEMO path — deterministic, zero LLM calls, demo/test safe.
        CeoAnalysisResponse response = deterministicAnalysis(ctx);
        applyToStartup(ctx, response, "mock", false);
        applyRoadmap(ctx, response, false);
        s.setCeoAnalysisFailed(false);
        s.setCeoAnalysisError(null);
        return CeoAnalysisOutcome.mock(response, response.executiveSummary());
    }

    private CeoAnalysisResponse callLlm(StartupContext ctx) {
        CeoAnalysisResponse raw = llm.generateStructured(systemPrompt, buildUserPrompt(ctx), CeoAnalysisResponse.class);
        if (raw == null || !raw.isValid()) {
            throw new CeoAnalysisException("CEO analysis response failed validation (missing required fields).");
        }
        return raw.normalized();
    }

    /** Compose the user prompt with the idea and any context the departments filled in. */
    String buildUserPrompt(StartupContext ctx) {
        Startup s = ctx.getStartup();
        StringBuilder sb = new StringBuilder();
        sb.append("Startup name: ").append(s.getName()).append('\n');
        sb.append("Founder's idea: ").append(IdeaAnalyzer.oneLine(ctx.idea())).append('\n');
        if (notBlank(s.getTargetAudience())) {
            sb.append("Working notes — target audience: ").append(s.getTargetAudience()).append('\n');
        }
        if (notBlank(s.getBusinessModel())) {
            sb.append("Working notes — business model so far: ").append(s.getBusinessModel()).append('\n');
        }
        sb.append("\nProduce the CEO analysis as JSON per the required format.");
        return sb.toString();
    }

    /**
     * Write a validated/normalised analysis into the authoritative startup state.
     * The {@code real} flag marks LLM-authored output: in that case we do NOT apply
     * the deterministic {@code marketReadiness} floor — the simulation's numeric
     * state should reflect the model's reasoning, not a hard-coded minimum. The
     * floor is retained for the scripted/offline path to preserve Phase 1 output.
     */
    private void applyToStartup(StartupContext ctx, CeoAnalysisResponse r, String provider, boolean real) {
        Startup s = ctx.getStartup();
        // Problem and the strategic fields below are unambiguously CEO-owned.
        s.setProblem(r.problem());
        s.setExecutiveSummary(r.executiveSummary());
        s.setMvpDirection(r.recommendedMvpDirection());
        s.setStrategicObjectives(joinLines(r.strategicObjectives()));
        s.setAssumptions(joinLines(r.assumptions()));
        s.setCeoAnalysisProvider(provider);
        if (!real) {
            s.setMarketReadiness(Math.max(s.getMarketReadiness(), 55));
        }

        // targetAudience / valueProposition / businessModel are also touched by
        // the (still-mock) Marketing agent, which runs before the CEO. To avoid
        // changing Phase 1 blueprint output we only fill these when a department
        // has not already set them. The CEO's own view is always preserved in
        // the dedicated strategic fields above and in the analysis response.
        if (notBlank(r.targetCustomer()) && !notBlank(s.getTargetAudience())) {
            s.setTargetAudience(r.targetCustomer());
        }
        if (notBlank(r.valueProposition()) && !notBlank(s.getValueProposition())) {
            s.setValueProposition(r.valueProposition());
        }
        if (notBlank(r.businessModel()) && !notBlank(s.getBusinessModel())) {
            s.setBusinessModel(r.businessModel());
        }
        r.risks().forEach(ctx::addRisk);
    }

    /**
     * Deterministic CEO analysis — the Phase 1 heuristic reasoning, now shaped
     * into a {@link CeoAnalysisResponse}. Used offline and as the failure
     * fallback so behavior is stable without an API key.
     */
    CeoAnalysisResponse deterministicAnalysis(StartupContext ctx) {
        String audience = IdeaAnalyzer.audience(ctx.idea());
        String domain = IdeaAnalyzer.domain(ctx.idea());
        String noun = IdeaAnalyzer.productNoun(ctx.idea());

        String problem = capitalize(audience) + " struggle to get trustworthy, personalised guidance in "
                + domain + " — existing options are generic, slow, or hard to trust.";
        String businessModel = notBlank(ctx.getStartup().getBusinessModel())
                ? ctx.getStartup().getBusinessModel()
                : "Freemium SaaS with a Pro subscription.";
        String valueProp = "Personalised " + domain + " results in seconds, delivered the moment you need them.";
        String solution = "A focused " + noun + " that gives " + audience
                + " fast, personalised results in " + domain + ".";

        return new CeoAnalysisResponse(
                "Become the default personalised entry point for " + audience + " in " + domain
                        + ", launching a sharp MVP within roughly three months while protecting runway.",
                problem,
                capitalize(audience),
                solution,
                valueProp,
                businessModel,
                List.of("Ship a focused MVP fast", "Maximise time-to-value",
                        "Protect runway", "Build a word-of-mouth loop"),
                List.of("Target users feel this pain acutely enough to try a new product",
                        "A freemium model can convert at a viable rate"),
                List.of("Underserved niche with generic incumbents",
                        "A delightful first-run experience can drive organic growth"),
                List.of("Adoption risk if the core value is not immediately obvious",
                        "Scope creep could delay the launch"),
                "Start with the core personalised engine and onboarding; defer non-essential social features to V2."
        ).normalized();
    }

    @Override
    public String debateStatement(StartupContext ctx) {
        return say("I propose launching the MVP within 3 months. Let's hear the trade-offs before I decide.");
    }

    /**
     * Resolve the scope debate: cut the Community feed from V1 (keeping the
     * product scanner), which shortens the timeline and protects runway.
     * Mutates the shared context accordingly.
     */
    public DecisionOutcome resolveDebate(StartupContext ctx) {
        ctx.getMvpFeatures().stream()
                .filter(f -> f.getName().equalsIgnoreCase("Community feed"))
                .forEach(f -> {
                    f.setInMvp(false);
                    f.setTargetRelease("V2");
                });

        Startup s = ctx.getStartup();
        s.setConstraints("Launch target: ~3 months. V1 excludes the Community feed; product scanner retained.");

        String decision = "Remove the Community feed from the MVP (move to V2) and retain the product scanner; "
                + "target a ~3 month launch.";
        String reason = "Reduces development complexity and initial cost, protects runway, and keeps the "
                + "feature most important to positioning.";
        return new DecisionOutcome(decision, reason,
                List.of(AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE));
    }

    /** Final synthesised pitch used in the blueprint. */
    public String pitch(StartupContext ctx) {
        Startup s = ctx.getStartup();
        return s.getName() + " helps " + s.getTargetAudience() + " " + lowerFirst(valueTail(s))
                + " We start with a focused, delightful MVP and a freemium model built to spread by word-of-mouth.";
    }

    /**
     * Persist the roadmap. In REAL mode, when the LLM supplied its own roadmap
     * entries they are authoritative and written as milestones. Otherwise (scripted
     * mode, or REAL with no roadmap in the response) a deterministic scaffold is
     * used. Idempotent: a retry never duplicates milestones.
     */
    private void applyRoadmap(StartupContext ctx, CeoAnalysisResponse r, boolean real) {
        if (!ctx.getRoadmap().isEmpty()) {
            return; // idempotent — retry must not duplicate milestones
        }
        if (real && r != null && r.roadmap() != null && !r.roadmap().isEmpty()) {
            int order = 0;
            for (CeoAnalysisResponse.RoadmapEntry e : r.roadmap()) {
                if (e == null || !e.isUsable()) {
                    continue;
                }
                CeoAnalysisResponse.RoadmapEntry t = e.trimmed();
                ctx.addMilestone(new RoadmapMilestone(ctx.startupId(), t.phase(), t.title(),
                        t.description(), t.timeframe(), order++));
            }
            if (order > 0) {
                return; // the model's roadmap is authoritative
            }
            // no usable entries — fall through to the deterministic scaffold
        }
        buildRoadmapScaffold(ctx);
    }

    private void buildRoadmapScaffold(StartupContext ctx) {
        if (!ctx.getRoadmap().isEmpty()) {
            return; // idempotent — retry must not duplicate milestones
        }
        ctx.addMilestone(new RoadmapMilestone(ctx.startupId(), "Discovery", "Validate & design",
                "Interviews, prototype and scope lock.", "Weeks 1-2", 0));
        ctx.addMilestone(new RoadmapMilestone(ctx.startupId(), "Build", "MVP engineering",
                "Core engine, onboarding and product scanner.", "Weeks 3-10", 1));
        ctx.addMilestone(new RoadmapMilestone(ctx.startupId(), "Beta", "Closed beta",
                "Ship to a focused beachhead and iterate on feedback.", "Weeks 11-12", 2));
        ctx.addMilestone(new RoadmapMilestone(ctx.startupId(), "Launch", "Public launch",
                "Open sign-ups, turn on the referral loop.", "Month 4", 3));
        ctx.addMilestone(new RoadmapMilestone(ctx.startupId(), "Growth", "Scale & V2",
                "Expand segments and build out post-launch features.", "Months 5-9", 4));
    }

    private String loadSystemPrompt() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Could not load CEO system prompt from {}", SYSTEM_PROMPT_PATH, e);
            // A minimal inline fallback so the agent is never left promptless.
            return "You are the CEO of a new startup. Analyze the idea and respond with a single JSON object "
                    + "containing: executiveSummary, problem, targetCustomer, proposedSolution, valueProposition, "
                    + "businessModel, strategicObjectives (array), assumptions (array), opportunities (array), "
                    + "risks (array), recommendedMvpDirection. Return only JSON.";
        }
    }

    private static String shortError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    private static String joinLines(List<String> items) {
        return items == null || items.isEmpty() ? null : String.join("\n", items);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String valueTail(Startup s) {
        String vp = s.getValueProposition();
        return vp == null ? "get personalised results fast." : vp;
    }

    private String lowerFirst(String v) {
        return v.isEmpty() ? v : Character.toLowerCase(v.charAt(0)) + v.substring(1);
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
