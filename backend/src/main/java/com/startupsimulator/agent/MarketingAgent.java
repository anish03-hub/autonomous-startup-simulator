package com.startupsimulator.agent;

import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.MarketingPlan;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Marketing department: audience, competitors, positioning, pricing, channels
 * and go-to-market.
 *
 * <p>Phase 2B: the analysis is produced by a real LLM through the
 * {@link LLMService} seam into a typed {@link MarketingAnalysisResponse}, built
 * on the CEO's strategic analysis and the Development scope (both injected via
 * the user prompt). Falls back to the deterministic Phase 1 analysis when no
 * real provider is configured or a real call fails.
 */
@Component
public class MarketingAgent extends DepartmentAgent {

    public MarketingAgent(LLMService llm, LlmProperties llmProperties) {
        super(llm, llmProperties, "prompts/marketing-system-prompt.txt");
    }

    @Override
    public AgentType type() {
        return AgentType.MARKETING;
    }

    @Override
    public String initialObjective(StartupContext ctx) {
        return "Research the market and define positioning and go-to-market.";
    }

    @Override
    public List<String> plannedSubtasks() {
        return List.of("Audience research", "Competitor analysis", "Positioning",
                "Pricing", "Channel plan");
    }

    // ---- Real (LLM) path ----------------------------------------------------

    @Override
    protected String applyRealAnalysis(StartupContext ctx) {
        MarketingAnalysisResponse raw =
                llm.generateStructured(systemPrompt, buildUserPrompt(ctx), MarketingAnalysisResponse.class);
        if (raw == null || !raw.isValid()) {
            throw new AgentAnalysisException("Marketing analysis response failed validation.");
        }
        MarketingAnalysisResponse r = raw.normalized();

        MarketingPlan mp = ctx.getMarketingPlan();
        mp.setPositioning(r.positioning());
        mp.setCompetitorAnalysis(r.competitorAnalysis());
        mp.setPricingStrategy(r.pricingStrategy());
        mp.setChannels(joinCsv(r.channels()));
        mp.setGoToMarket(r.goToMarket());

        Startup s = ctx.getStartup();
        if (notBlank(r.targetAudience())) {
            s.setTargetAudience(capitalize(r.targetAudience()));
        }
        if (notBlank(r.valueProposition())) {
            s.setValueProposition(r.valueProposition());
        }
        if (notBlank(r.businessModel())) {
            s.setBusinessModel(r.businessModel());
        }

        r.risks().forEach(ctx::addRisk);

        // Phase 5B: queue any agent-driven memory-creation intents (persisted only
        // after this real analysis succeeds, by the memory-aware entry point).
        ctx.queueMemoryIntents(type(), r.memoryIntents());

        // REAL mode: the model's positioning is authoritative. We no longer floor
        // marketReadiness or append a hard-coded "keep the product scanner" line —
        // any product constraint must come from the model (or the explicit prompt
        // constraint in the boardroom), not from a post-processing override.
        return "Positioning: " + r.positioning();
    }

    // ---- Deterministic (Phase 1) path & fallback ----------------------------

    @Override
    protected String applyDeterministicAnalysis(StartupContext ctx) {
        String audience = IdeaAnalyzer.audience(ctx.idea());
        String domain = IdeaAnalyzer.domain(ctx.idea());

        MarketingPlan mp = ctx.getMarketingPlan();
        mp.setPositioning("The fastest way for " + audience + " to get trustworthy, personalised results in "
                + domain + " — the product scanner makes it feel magical on day one.");
        mp.setCompetitorAnalysis("Incumbents are generic and manual; niche apps lack personalisation. "
                + "Our wedge is a delightful scanner + tailored recommendations.");
        mp.setPricingStrategy("Freemium: free core with a $9/mo Pro tier for advanced recommendations and history.");
        mp.setChannels("Content/SEO, short-form video, targeted communities, referral loop, a few micro-influencers");
        mp.setGoToMarket("Launch to a focused beachhead of " + audience
                + ", drive word-of-mouth via the scanner, then expand adjacent segments.");

        Startup s = ctx.getStartup();
        s.setTargetAudience(capitalize(audience));
        s.setValueProposition("Personalised " + domain + " results in seconds, not hours — starting the moment you scan.");
        s.setBusinessModel("Freemium SaaS with a Pro subscription; later, affiliate/partner revenue.");
        s.setMarketReadiness(Math.max(s.getMarketReadiness(), 60));

        ctx.addRisk("Acquisition risk: freemium conversion must clear ~3-5% for the model to work.");
        return say("The product scanner should remain in V1 — it is core to our positioning and the main driver of "
                + "word-of-mouth. Community can wait.");
    }

    @Override
    protected void recordAnalysisMeta(StartupContext ctx, String provider, boolean failed, String error) {
        MarketingPlan mp = ctx.getMarketingPlan();
        mp.setAnalysisProvider(provider);
        mp.setAnalysisFailed(failed);
        mp.setAnalysisError(error);
    }

    @Override
    public String debateStatement(StartupContext ctx) {
        return say("Keep the product scanner in V1 — it is central to how we differentiate and how users spread the word.");
    }

    /** Compose the user prompt: CEO analysis + the Development scope, then the idea. */
    String buildUserPrompt(StartupContext ctx) {
        Startup s = ctx.getStartup();
        StringBuilder sb = new StringBuilder();
        sb.append(ceoContextBlock(ctx)).append('\n');
        String memory = memoryBlock(ctx);
        if (!memory.isEmpty()) {
            sb.append(memory).append('\n');
        }
        sb.append("=== DEVELOPMENT MVP SCOPE (already decided) ===\n");
        appendIf(sb, "Architecture", ctx.getTechnicalPlan().getArchitecture());
        appendIf(sb, "Timeline", ctx.getTechnicalPlan().getTimeline());
        appendIf(sb, "Product summary", s.getSolution());
        sb.append('\n');
        sb.append("Startup name: ").append(s.getName()).append('\n');
        sb.append("Founder's idea: ").append(IdeaAnalyzer.oneLine(ctx.idea())).append('\n');
        sb.append("\nProduce the Marketing analysis as JSON per the required format, consistent with the CEO's "
                + "business model and the product above.");
        return sb.toString();
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
