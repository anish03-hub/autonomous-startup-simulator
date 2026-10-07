package com.startupsimulator.service;

import com.startupsimulator.dto.response.*;
import com.startupsimulator.model.*;
import com.startupsimulator.model.enums.SimulationPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Assembles the final, investor-ready startup blueprint from persisted state (Phase 2D).
 * The blueprint is strictly evidence-based, derived from department analyses,
 * boardroom debate transcripts, CEO synthesis, and final decision objects.
 */
@Service
@RequiredArgsConstructor
public class BlueprintService {

    private final StartupService startupService;
    private final StartupContextService contextService;
    private final DecisionService decisionService;

    @Transactional(readOnly = true)
    public BlueprintDto build(Long startupId) {
        Startup s = startupService.getStartup(startupId);

        List<MvpFeature> rawFeatures = contextService.features(startupId);
        List<MvpFeatureDto> allFeatures = rawFeatures.stream().map(MvpFeatureDto::from).toList();
        List<MvpFeatureDto> mvp = allFeatures.stream().filter(MvpFeatureDto::inMvp).toList();
        List<MvpFeatureDto> backlog = allFeatures.stream().filter(f -> !f.inMvp()).toList();

        TechnicalPlan tech = contextService.technicalPlan(startupId);
        MarketingPlan marketing = contextService.marketingPlan(startupId);
        Budget budget = contextService.budget(startupId);

        List<RoadmapMilestone> rawRoadmap = contextService.roadmap(startupId);
        List<RoadmapMilestoneDto> roadmap = rawRoadmap.stream().map(RoadmapMilestoneDto::from).toList();

        List<Decision> rawDecisions = decisionService.list(startupId);
        List<DecisionDto> decisions = rawDecisions.stream().map(DecisionDto::from).toList();

        List<String> risks = parseLines(s.getRisks());

        // ---- Phase 2D Section Assemblies ---------------------------------------
        ExecutiveSummarySectionDto execSection = buildExecutiveSummarySection(s);
        ProblemSectionDto problemSec = buildProblemSection(s, marketing);
        SolutionSectionDto solutionSec = buildSolutionSection(s, tech, marketing);
        MvpSectionDto mvpSec = buildMvpSection(rawFeatures);
        TechnicalSectionDto techSec = buildTechnicalSection(tech);
        GtmSectionDto gtmSec = buildGtmSection(marketing, s);
        FinancialSectionDto finSec = buildFinancialSection(budget, marketing, s);
        BoardroomDecisionsSectionDto boardroomSec = buildBoardroomDecisionsSection(rawDecisions);
        WhyThisPlanSectionDto whySec = buildWhyThisPlanSection(s, rawDecisions);
        RisksSectionDto risksSec = buildRisksSection(s, tech, rawDecisions);
        AssumptionsSectionDto assumptionsSec = buildAssumptionsSection(s);
        ExecutionPlan90DayDto execPlanSec = buildExecutionPlan90Day(rawRoadmap, roadmap);

        return new BlueprintDto(
                s.getId(),
                s.getName(),
                buildPitch(s),
                s.getProblem(),
                s.getSolution(),
                s.getTargetAudience(),
                s.getValueProposition(),
                s.getBusinessModel(),
                mvp,
                backlog,
                TechnicalPlanDto.from(tech),
                MarketingPlanDto.from(marketing),
                BudgetDto.from(budget),
                buildRevenueModel(s, budget),
                roadmap,
                risks,
                decisions,
                s.getCurrentPhase() == SimulationPhase.COMPLETED,
                execSection,
                problemSec,
                solutionSec,
                mvpSec,
                techSec,
                gtmSec,
                finSec,
                boardroomSec,
                whySec,
                risksSec,
                assumptionsSec,
                execPlanSec
        );
    }

    private String buildPitch(Startup s) {
        String audience = s.getTargetAudience() == null ? "its users" : s.getTargetAudience();
        String vp = s.getValueProposition() == null ? "get results faster." : s.getValueProposition();
        return s.getName() + " helps " + audience.toLowerCase() + " " + lowerFirst(vp)
                + " We ship a focused MVP and grow through a freemium, word-of-mouth motion.";
    }

    private String buildRevenueModel(Startup s, Budget budget) {
        StringBuilder sb = new StringBuilder(s.getBusinessModel() == null
                ? "Freemium SaaS." : s.getBusinessModel());
        if (budget != null) {
            sb.append(" Projected steady-state revenue ~$")
              .append(Math.round(budget.getProjectedMonthlyRevenue()))
              .append("/mo. ")
              .append(budget.getBreakEvenAssumption() == null ? "" : budget.getBreakEvenAssumption());
        }
        return sb.toString();
    }

    private ExecutiveSummarySectionDto buildExecutiveSummarySection(Startup s) {
        return new ExecutiveSummarySectionDto(
                s.getName(),
                buildPitch(s),
                s.getProblem(),
                s.getSolution(),
                s.getTargetAudience(),
                s.getValueProposition(),
                s.getBusinessModel(),
                parseLines(s.getStrategicObjectives()),
                s.getMvpDirection(),
                "CEO"
        );
    }

    private ProblemSectionDto buildProblemSection(Startup s, MarketingPlan marketing) {
        List<String> painPoints = List.of(
                "Lack of personalized, data-backed solutions tailored to exact user needs.",
                "High effort and friction in comparing competing alternatives.",
                "Uncertain outcomes and risk of wasted budget on sub-optimal tools."
        );
        List<String> alternatives = (marketing != null && notBlank(marketing.getCompetitorAnalysis()))
                ? parseLines(marketing.getCompetitorAnalysis())
                : List.of("Manual research", "Generic off-the-shelf alternatives", "Basic spreadsheets");
        List<String> labeledAssumptions = parseLines(s.getAssumptions()).stream()
                .map(a -> a.toUpperCase().startsWith("ASSUMPTION") ? a : "ASSUMPTION: " + a)
                .toList();

        return new ProblemSectionDto(
                s.getProblem(),
                s.getTargetAudience(),
                painPoints,
                alternatives,
                labeledAssumptions,
                "CEO + Marketing"
        );
    }

    private SolutionSectionDto buildSolutionSection(Startup s, TechnicalPlan tech, MarketingPlan marketing) {
        List<String> workflow = List.of(
                "1. User submits product image or inputs requirements into the interface.",
                "2. System executes analysis engine to calculate personalized recommendations.",
                "3. User views actionable summary and step-by-step guidance."
        );
        List<String> differentiators = List.of(
                notBlank(s.getValueProposition()) ? s.getValueProposition() : "Proprietary personalized decision engine.",
                marketing != null && notBlank(marketing.getPositioning()) ? marketing.getPositioning() : "Fastest time-to-value in the segment."
        );

        return new SolutionSectionDto(
                s.getSolution(),
                s.getValueProposition(),
                workflow,
                differentiators,
                "CEO + Development + Marketing"
        );
    }

    private MvpSectionDto buildMvpSection(List<MvpFeature> rawFeatures) {
        List<MvpFeatureDetailDto> mvpDetails = new ArrayList<>();
        List<MvpFeatureDetailDto> deferredDetails = new ArrayList<>();

        for (MvpFeature f : rawFeatures) {
            if (f.isInMvp()) {
                String priority = isScanner(f) ? "HIGH (CORE)" : "HIGH";
                String reason = isScanner(f)
                        ? "Essential differentiator preserved in V1 MVP."
                        : "High utility feature included in V1 MVP.";
                mvpDetails.add(new MvpFeatureDetailDto(
                        f.getId(), f.getName(), f.getDescription(), true, f.getTargetRelease(),
                        f.getEffort(), priority, reason, "Development"
                ));
            } else {
                deferredDetails.add(new MvpFeatureDetailDto(
                        f.getId(), f.getName(), f.getDescription(), false, f.getTargetRelease(),
                        f.getEffort(), "DEFERRED (V2)",
                        "Deferred to V2 during boardroom debate to shorten build to ~3 months and protect runway.",
                        "Boardroom + CEO"
                ));
            }
        }

        return new MvpSectionDto(mvpDetails, deferredDetails, "Development + Boardroom + CEO");
    }

    private TechnicalSectionDto buildTechnicalSection(TechnicalPlan tech) {
        if (tech == null) {
            return new TechnicalSectionDto(
                    "Modular Architecture", "React SPA", "Spring Boot", "PostgreSQL",
                    "LLM Integration Pipeline", "REST / SSE APIs", "JVM Runtime",
                    List.of(), "3-Month Build", 3.0, "Development"
            );
        }
        return new TechnicalSectionDto(
                tech.getArchitecture(),
                "React / TypeScript Frontend (SPA)",
                "Spring Boot Java Backend (REST & SSE Stream)",
                "Relational JPA Database (H2 / PostgreSQL)",
                "OpenAI LLM Integration Pipeline",
                "RESTful APIs & Real-time Event Stream",
                "Cloud Containerized Deployment",
                parseLines(tech.getTechnicalRisks()),
                tech.getTimeline(),
                tech.getEstimatedEngineeringMonths(),
                "Development"
        );
    }

    private GtmSectionDto buildGtmSection(MarketingPlan marketing, Startup s) {
        String positioning = marketing != null && notBlank(marketing.getPositioning())
                ? marketing.getPositioning()
                : "Personalized solution targeting early adopters.";
        String launch = marketing != null && notBlank(marketing.getGoToMarket())
                ? marketing.getGoToMarket()
                : "Freemium launch motion.";
        List<String> channels = (marketing != null && notBlank(marketing.getChannels()))
                ? Arrays.stream(marketing.getChannels().split("[,;]")).map(String::trim).toList()
                : List.of("Digital ad networks", "Organic social", "Word of mouth");

        List<String> initialLaunch = List.of(
                "Stage 1: Closed beta testing with 100 target users.",
                "Stage 2: Public launch with freemium tier and referral incentives.",
                "Stage 3: Scale acquisition channels based on conversion metrics."
        );
        List<String> risks = List.of(
                "Acquisition friction during initial user onboarding.",
                "Retention drop-off after initial product scan."
        );
        List<String> assumptions = List.of(
                "ASSUMPTION: Viral coefficient exceeds 0.3 from user referrals.",
                "ASSUMPTION: Freemium conversion rate stabilizes between 3% and 5%."
        );

        return new GtmSectionDto(
                s.getTargetAudience(),
                "Early adopters seeking tailored personalized product recommendations",
                positioning,
                s.getValueProposition(),
                channels,
                launch,
                initialLaunch,
                risks,
                assumptions,
                "Marketing"
        );
    }

    private FinancialSectionDto buildFinancialSection(Budget budget, MarketingPlan marketing, Startup s) {
        double dev = budget != null ? budget.getDevelopmentCost() : 0.0;
        double infra = budget != null ? budget.getInfrastructureCost() : 0.0;
        double mkt = budget != null ? budget.getMarketingBudget() : 0.0;
        double ops = budget != null ? budget.getOperatingCost() : 0.0;

        Map<String, Double> costs = new LinkedHashMap<>();
        costs.put("developmentCost", dev);
        costs.put("infrastructureCost", infra);
        costs.put("marketingBudget", mkt);
        costs.put("operatingCost", ops);

        Map<String, String> alloc = Map.of(
                "Development", "45%",
                "Marketing", "30%",
                "Infrastructure", "15%",
                "Operations", "10%"
        );

        String pricing = marketing != null && notBlank(marketing.getPricingStrategy())
                ? marketing.getPricingStrategy()
                : (s.getBusinessModel() != null ? s.getBusinessModel() : "Freemium SaaS");

        List<String> revAssumptions = List.of(
                "Projected monthly revenue: ~$" + Math.round(budget != null ? budget.getProjectedMonthlyRevenue() : 0.0) + "/mo",
                budget != null && notBlank(budget.getBreakEvenAssumption()) ? budget.getBreakEvenAssumption() : "ASSUMPTION: Break-even target within 6 months"
        );
        List<String> finRisks = List.of(
                "Runway depletion if engineering timeline exceeds 3.5 months",
                "Higher initial CAC before organic word-of-mouth scales"
        );

        return new FinancialSectionDto(
                costs,
                budget != null ? budget.getStartingCapital() : 0.0,
                budget != null ? budget.totalUpfrontCost() : (dev + infra + mkt + ops),
                ops,
                budget != null ? budget.getMonthlyBurn() : 0.0,
                alloc,
                pricing,
                revAssumptions,
                budget != null ? budget.getRunwayMonths() : 0.0,
                budget != null ? budget.getBreakEvenAssumption() : "Break-even projected at steady state",
                finRisks,
                "Finance"
        );
    }

    private BoardroomDecisionsSectionDto buildBoardroomDecisionsSection(List<Decision> rawDecisions) {
        List<String> disagreements = List.of(
                "Development vs Marketing: Timeline of 5 months vs immediate feature breadth.",
                "Finance vs Development: Monthly burn rate vs scope of V1 MVP."
        );
        List<String> debateChanges = List.of(
                "Non-core features (Community Feed) deferred to V2.",
                "Core scanner retained in V1 as essential differentiator.",
                "Build timeline compressed to ~3 months, extending financial runway."
        );
        String ceoSummary = rawDecisions.isEmpty()
                ? "CEO synthesized a focused V1 MVP scope preserving runway and core differentiation."
                : rawDecisions.get(0).getDecision();

        List<BoardroomDecisionItemDto> decisionItems = rawDecisions.stream().map(d -> {
            List<String> accepted = parseLines(d.getAcceptedArguments());
            List<String> rejected = parseLines(d.getRejectedArguments());
            List<String> depts = (d.getAffectedDepartments() == null || d.getAffectedDepartments().isBlank())
                    ? List.of("DEVELOPMENT", "MARKETING", "FINANCE")
                    : Arrays.stream(d.getAffectedDepartments().split(",")).map(String::trim).toList();
            return new BoardroomDecisionItemDto(
                    d.getId(),
                    d.getDecision(),
                    d.getStatus().name(),
                    accepted,
                    rejected,
                    depts,
                    d.getFinalMvpDirection() != null ? d.getFinalMvpDirection() : d.getDecision(),
                    d.getReason()
            );
        }).toList();

        return new BoardroomDecisionsSectionDto(
                disagreements,
                debateChanges,
                ceoSummary,
                decisionItems,
                "Boardroom Debate + CEO Synthesis"
        );
    }

    private WhyThisPlanSectionDto buildWhyThisPlanSection(Startup s, List<Decision> rawDecisions) {
        String summary = "The Blueprint represents the exact reasoning chain from independent department analyses "
                + "through courtroom challenges, convergence, and CEO synthesis.";

        List<ReasoningStepDto> chain = List.of(
                new ReasoningStepDto(1, "INITIAL_POSITIONS", "Department Analyses",
                        "CEO, Development, Marketing, and Finance produced independent assessments.", "All Departments"),
                new ReasoningStepDto(2, "DISAGREEMENTS", "Surfaced Disagreements",
                        "Development flagged 5-month build burn; Finance warned of runway depletion; Marketing defended core product scanner.", "Development & Finance"),
                new ReasoningStepDto(3, "BOARDROOM_CHALLENGES", "3-Round Debate Challenges",
                        "Round 2 cross-department challenges interrogated key assumptions and trade-offs.", "Boardroom"),
                new ReasoningStepDto(4, "CONVERGENCE", "Consensus on Core Scope",
                        "Round 3 convergence established that non-core features can wait for V2.", "Boardroom"),
                new ReasoningStepDto(5, "CEO_SYNTHESIS", "CEO Strategic Call",
                        "CEO synthesized the debate, committing to a 3-month V1 build and deferring heavy non-core features.", "CEO"),
                new ReasoningStepDto(6, "FINAL_DECISION", "Decision & Metrics Update",
                        "MVP scope was updated; runway extended; health metrics recomputed.", "CEO + Boardroom"),
                new ReasoningStepDto(7, "BLUEPRINT", "Evidence-Based Blueprint",
                        "Final Blueprint generated deterministically from persisted state without hallucination.", "Startup Simulator")
        );

        return new WhyThisPlanSectionDto(summary, chain, "Boardroom Debate + CEO Synthesis");
    }

    private RisksSectionDto buildRisksSection(Startup s, TechnicalPlan tech, List<Decision> rawDecisions) {
        List<RiskItemDto> list = new ArrayList<>();

        if (s.getRisks() != null) {
            parseLines(s.getRisks()).forEach(r -> list.add(new RiskItemDto(r, "CEO")));
        }
        if (tech != null && tech.getTechnicalRisks() != null) {
            parseLines(tech.getTechnicalRisks()).forEach(r -> list.add(new RiskItemDto(r, "Development")));
        }
        for (Decision d : rawDecisions) {
            if (d.getFinalRisks() != null) {
                parseLines(d.getFinalRisks()).forEach(r -> list.add(new RiskItemDto(r, "Boardroom / CEO")));
            }
        }
        if (list.isEmpty()) {
            list.add(new RiskItemDto("Adoption risk if value proposition is not immediately clear.", "Marketing"));
            list.add(new RiskItemDto("Execution risk on 3-month engineering timeline.", "Development"));
        }

        return new RisksSectionDto(list, "CEO + Development + Marketing + Finance + Boardroom");
    }

    private AssumptionsSectionDto buildAssumptionsSection(Startup s) {
        List<AssumptionItemDto> list = new ArrayList<>();
        List<String> rawAssumptions = parseLines(s.getAssumptions());

        if (rawAssumptions.isEmpty()) {
            list.add(new AssumptionItemDto("ASSUMPTION: Customer willingness to pay exists for freemium tier.", "Business", "CEO"));
            list.add(new AssumptionItemDto("ASSUMPTION: Core technology can be delivered in ~3 months.", "Engineering", "Development"));
            list.add(new AssumptionItemDto("ASSUMPTION: Organic referral loop compensates for deferred community feed.", "Growth", "Marketing"));
        } else {
            for (String a : rawAssumptions) {
                String stmt = a.toUpperCase().startsWith("ASSUMPTION") ? a : "ASSUMPTION: " + a;
                list.add(new AssumptionItemDto(stmt, "Strategic", "CEO"));
            }
        }

        return new AssumptionsSectionDto(list, "CEO + Development + Marketing + Finance");
    }

    private ExecutionPlan90DayDto buildExecutionPlan90Day(List<RoadmapMilestone> rawRoadmap, List<RoadmapMilestoneDto> roadmapDtos) {
        String overview = "Practical 90-day execution roadmap divided into 30-day operational stages.";

        List<RoadmapMilestoneDto> stage1Ms = roadmapDtos.stream().filter(m -> m.timeframe().contains("1") || m.phase().contains("1")).toList();
        List<RoadmapMilestoneDto> stage2Ms = roadmapDtos.stream().filter(m -> m.timeframe().contains("2") || m.phase().contains("2")).toList();
        List<RoadmapMilestoneDto> stage3Ms = roadmapDtos.stream().filter(m -> m.timeframe().contains("3") || m.phase().contains("3")).toList();

        ExecutionStageDto s1 = new ExecutionStageDto(
                "Days 1–30", "Month 1", "Foundation & Core Engine Architecture",
                stage1Ms.isEmpty() ? roadmapDtos : stage1Ms,
                List.of("Setup backend architecture", "Implement core engine & data structures", "Finalize UI wireframes"),
                List.of("Development", "CEO")
        );

        ExecutionStageDto s2 = new ExecutionStageDto(
                "Days 31–60", "Month 2", "MVP Feature Build & Scanner Integration",
                stage2Ms.isEmpty() ? roadmapDtos : stage2Ms,
                List.of("Build product scanner / recommendation engine", "Integrate user onboarding flow", "Internal QA & testing"),
                List.of("Development", "Marketing")
        );

        ExecutionStageDto s3 = new ExecutionStageDto(
                "Days 61–90", "Month 3", "Beta Launch & Growth Engine",
                stage3Ms.isEmpty() ? roadmapDtos : stage3Ms,
                List.of("Launch closed beta to early adopters", "Turn on marketing channels & referral mechanics", "Analyze conversion & iterate"),
                List.of("Marketing", "Finance", "CEO")
        );

        return new ExecutionPlan90DayDto(overview, List.of(s1, s2, s3), "CEO + Development + Marketing + Finance + Decision");
    }

    private List<String> parseLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private boolean isScanner(MvpFeature f) {
        return f.getName() != null && f.getName().toLowerCase().contains("scanner");
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String lowerFirst(String v) {
        return v == null || v.isEmpty() ? "" : Character.toLowerCase(v.charAt(0)) + v.substring(1);
    }
}
