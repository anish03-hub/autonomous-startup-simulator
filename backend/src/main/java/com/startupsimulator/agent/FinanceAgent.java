package com.startupsimulator.agent;

import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.Budget;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Finance department: costs, budget, revenue assumptions, burn rate, runway and
 * financial risk.
 *
 * <p>Phase 2B: the analysis is produced by a real LLM through the
 * {@link LLMService} seam into a typed {@link FinanceAnalysisResponse}, built on
 * the CEO direction, the Development timeline and the Marketing pricing (all
 * injected via the user prompt). Falls back to the deterministic Phase 1 budget
 * when no real provider is configured or a real call fails. Reacts to the
 * boardroom scope decision via {@link #applyScopeReduction}.
 */
@Component
public class FinanceAgent extends DepartmentAgent {

    private static final double STARTING_CAPITAL = 60_000d;

    public FinanceAgent(LLMService llm, LlmProperties llmProperties) {
        super(llm, llmProperties, "prompts/finance-system-prompt.txt");
    }

    @Override
    public AgentType type() {
        return AgentType.FINANCE;
    }

    @Override
    public String initialObjective(StartupContext ctx) {
        return "Model the budget, burn rate and runway.";
    }

    @Override
    public List<String> plannedSubtasks() {
        return List.of("Cost breakdown", "Marketing budget", "Revenue assumptions",
                "Burn & runway", "Financial risks");
    }

    // ---- Real (LLM) path ----------------------------------------------------

    @Override
    protected String applyRealAnalysis(StartupContext ctx) {
        FinanceAnalysisResponse raw =
                llm.generateStructured(systemPrompt, buildUserPrompt(ctx), FinanceAnalysisResponse.class);
        if (raw == null || !raw.isValid()) {
            throw new AgentAnalysisException("Finance analysis response failed validation.");
        }
        FinanceAnalysisResponse r = raw.normalized();

        Budget b = ctx.getBudget();
        b.setStartingCapital(r.startingCapital() == null ? STARTING_CAPITAL : r.startingCapital());
        b.setDevelopmentCost(r.developmentCost());
        b.setMarketingBudget(r.marketingBudget());
        b.setInfrastructureCost(r.infrastructureCost());
        b.setOperatingCost(r.operatingCost());
        b.setMonthlyBurn(r.monthlyBurn());
        b.setProjectedMonthlyRevenue(r.projectedMonthlyRevenue());
        b.setBreakEvenAssumption(r.breakEvenAssumption());

        ctx.getStartup().setFinancialHealth(r.financialHealth());
        recomputeRunway(ctx);

        r.risks().forEach(ctx::addRisk);

        // Phase 3: a message-driven response. If the model chose to reply to a
        // department (e.g. Development, whose message it just read in its prompt),
        // queue that intent for the orchestrator to validate and persist.
        ctx.queueOutgoing(type(), r.messageIntent());

        // Phase 5B: queue any agent-driven memory-creation intents (persisted only
        // after this real analysis succeeds, by the memory-aware entry point).
        ctx.queueMemoryIntents(type(), r.memoryIntents());

        return "Budget modelled: upfront cost $" + fmt(b.totalUpfrontCost()) + ", leaving $"
                + fmt(b.getStartingCapital() - b.totalUpfrontCost()) + " and about "
                + b.getRunwayMonths() + " months of runway. " + r.breakEvenAssumption();
    }

    // ---- Deterministic (Phase 1) path & fallback ----------------------------

    @Override
    protected String applyDeterministicAnalysis(StartupContext ctx) {
        Budget b = ctx.getBudget();
        b.setStartingCapital(STARTING_CAPITAL);
        b.setDevelopmentCost(20_000d);   // full scope incl. community feed
        b.setMarketingBudget(10_000d);
        b.setInfrastructureCost(2_000d);
        b.setOperatingCost(1_600d);
        b.setMonthlyBurn(4_550d);
        b.setProjectedMonthlyRevenue(3_000d);
        b.setBreakEvenAssumption("Break-even around month 14 at ~1,000 paying users on the $9/mo Pro tier.");
        recomputeRunway(ctx);

        ctx.addRisk("Financial risk: at full scope, runway is under 6 months before a raise or revenue ramp.");
        return say("At the current scope, upfront cost is $33,600 leaving $26,400 and about 5.8 months of runway. "
                + "Trimming the heaviest non-core feature would materially extend that.");
    }

    @Override
    protected void recordAnalysisMeta(StartupContext ctx, String provider, boolean failed, String error) {
        Budget b = ctx.getBudget();
        b.setAnalysisProvider(provider);
        b.setAnalysisFailed(failed);
        b.setAnalysisError(error);
    }

    /** Recompute burn/runway after the boardroom trims scope (community -> V2). */
    public String applyScopeReduction(StartupContext ctx) {
        Budget b = ctx.getBudget();
        b.setDevelopmentCost(14_000d);   // community feed removed from V1
        b.setMonthlyBurn(3_900d);
        recomputeRunway(ctx);
        ctx.getStartup().setFinancialHealth(74);
        return say("With the Community feed moved to V2, upfront cost drops to $27,600, leaving $32,400 and "
                + "roughly 8.3 months of runway. Recalculated and looking healthier.");
    }

    /**
     * REAL-mode counterpart to {@link #applyScopeReduction}: recompute runway from
     * the authoritative, LLM-produced budget after the boardroom's scope decision,
     * <b>without</b> clamping development cost, burn or financial health to fixed
     * values. The LLM's financial figures survive the scope decision intact.
     */
    public String recomputeRunwayMessage(StartupContext ctx) {
        Budget b = ctx.getBudget();
        recomputeRunway(ctx);
        return say("Recalculated runway after the scope decision: upfront cost $" + fmt(b.totalUpfrontCost())
                + ", leaving $" + fmt(b.getStartingCapital() - b.totalUpfrontCost()) + " and about "
                + b.getRunwayMonths() + " months of runway.");
    }

    private void recomputeRunway(StartupContext ctx) {
        Budget b = ctx.getBudget();
        double remaining = b.getStartingCapital() - b.totalUpfrontCost();
        double runway = b.getMonthlyBurn() <= 0 ? 0 : remaining / b.getMonthlyBurn();
        b.setRunwayMonths(round1(runway));

        Startup s = ctx.getStartup();
        s.setBudgetRemaining(remaining);
        s.setRunwayMonths(round1(runway));
        if (s.getFinancialHealth() == 0) {
            s.setFinancialHealth(68);
        }
    }

    @Override
    public String debateStatement(StartupContext ctx) {
        return say("Five months of build increases projected initial costs and shortens runway. "
                + "I support reducing scope to protect the runway.");
    }

    /** Compose the user prompt: CEO analysis + the Development timeline + Marketing pricing. */
    String buildUserPrompt(StartupContext ctx) {
        Startup s = ctx.getStartup();
        StringBuilder sb = new StringBuilder();
        sb.append(ceoContextBlock(ctx)).append('\n');
        sb.append(communicationBlock(ctx, type())).append('\n');
        String memory = memoryBlock(ctx);
        if (!memory.isEmpty()) {
            sb.append(memory).append('\n');
        }
        sb.append("=== UPSTREAM PLANS (be consistent with these) ===\n");
        appendIf(sb, "Engineering timeline", ctx.getTechnicalPlan().getTimeline());
        if (ctx.getTechnicalPlan().getEstimatedEngineeringMonths() > 0) {
            appendIf(sb, "Estimated engineering months",
                    String.valueOf(ctx.getTechnicalPlan().getEstimatedEngineeringMonths()));
        }
        appendIf(sb, "Pricing strategy", ctx.getMarketingPlan().getPricingStrategy());
        appendIf(sb, "Go-to-market", ctx.getMarketingPlan().getGoToMarket());
        sb.append('\n');
        sb.append("Startup name: ").append(s.getName()).append('\n');
        sb.append("Founder's idea: ").append(IdeaAnalyzer.oneLine(ctx.idea())).append('\n');
        sb.append("\nProduce the Finance analysis as JSON per the required format. Larger engineering scope "
                + "should mean higher development cost and burn; keep the numbers internally consistent.");
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format("%,.0f", v);
    }
}
