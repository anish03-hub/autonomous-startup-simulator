package com.startupsimulator.service;

import com.startupsimulator.dto.response.*;
import com.startupsimulator.model.*;
import com.startupsimulator.model.enums.DecisionStatus;
import com.startupsimulator.model.enums.SimulationPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlueprintServiceTest {

    @Mock
    private StartupService startupService;

    @Mock
    private StartupContextService contextService;

    @Mock
    private DecisionService decisionService;

    @InjectMocks
    private BlueprintService blueprintService;

    private Startup startup;
    private TechnicalPlan techPlan;
    private MarketingPlan marketingPlan;
    private Budget budget;
    private List<MvpFeature> features;
    private List<RoadmapMilestone> roadmap;
    private List<Decision> decisions;

    @BeforeEach
    void setUp() {
        startup = new Startup("SkinAI", "An AI app for skincare recommendation.");
        startup.setId(100L);
        startup.setProblem("Users struggle to find appropriate skincare routines.");
        startup.setSolution("AI routine builder that scans products and analyzes skin needs.");
        startup.setTargetAudience("Skincare enthusiasts and beginners.");
        startup.setValueProposition("Personalized skincare routine in under 30 seconds.");
        startup.setBusinessModel("Freemium subscription");
        startup.setExecutiveSummary("SkinAI is an AI-powered skincare assistant.");
        startup.setStrategicObjectives("Ship focused V1 MVP\nProtect runway\nBuild viral growth loop");
        startup.setAssumptions("ASSUMPTION: Users trust AI recommendations for skincare\nASSUMPTION: Freemium conversion > 3%");
        startup.setRisks("Scanner accuracy risk\nAcquisition cost sensitivity");
        startup.setCurrentPhase(SimulationPhase.COMPLETED);

        techPlan = new TechnicalPlan(100L);
        techPlan.setArchitecture("Microservices API with AI Vision pipeline");
        techPlan.setTechStack("React Native, Spring Boot, PostgreSQL, OpenCV / PyTorch");
        techPlan.setTimeline("3-month build");
        techPlan.setTechnicalRisks("Model latency on mobile devices");
        techPlan.setEstimatedEngineeringMonths(3.0);

        marketingPlan = new MarketingPlan(100L);
        marketingPlan.setPositioning("AI Skincare Copilot");
        marketingPlan.setPricingStrategy("$9.99/mo premium tier");
        marketingPlan.setChannels("Instagram, TikTok, Organic search");
        marketingPlan.setCompetitorAnalysis("Manual routine apps, generic beauty blogs");
        marketingPlan.setGoToMarket("Freemium launch via social influencers");

        budget = new Budget(100L);
        budget.setDevelopmentCost(30000.0);
        budget.setInfrastructureCost(5000.0);
        budget.setMarketingBudget(10000.0);
        budget.setOperatingCost(5000.0);
        budget.setStartingCapital(80000.0);
        budget.setMonthlyBurn(12000.0);
        budget.setProjectedMonthlyRevenue(25000.0);
        budget.setRunwayMonths(5.5);
        budget.setBreakEvenAssumption("Break-even expected at month 7");

        MvpFeature f1 = new MvpFeature(100L, "AI Product Scanner", "Scan product barcodes and labels", true, 3);
        MvpFeature f2 = new MvpFeature(100L, "Personalized Routine Generator", "Generates daily morning/night routine", true, 2);
        MvpFeature f3 = new MvpFeature(100L, "Community Skincare Feed", "Social feed for sharing routines", false, 4);
        f3.setInMvp(false);
        f3.setTargetRelease("V2");
        features = List.of(f1, f2, f3);

        RoadmapMilestone m1 = new RoadmapMilestone(100L, "Phase 1", "Core Engine & Scanner", "Build core engine and scanner", "Months 1-2", 0);
        roadmap = List.of(m1);

        Decision d1 = new Decision(100L, 1L, "Defer Community Feed to V2 to shorten build to 3 months.", "Protects runway while preserving core scanner.");
        d1.setId(10L);
        d1.setStatus(DecisionStatus.APPROVED);
        d1.setAcceptedArguments("Scanner is core differentiator\nShort build protects burn");
        d1.setRejectedArguments("Full community social feed in V1");
        d1.setFinalRisks("Mobile scan latency");
        d1.setFinalPriorities("Focus on scanner & routine engine");
        d1.setAffectedDepartments("DEVELOPMENT,MARKETING,FINANCE");
        decisions = List.of(d1);

        when(startupService.getStartup(100L)).thenReturn(startup);
        when(contextService.features(100L)).thenReturn(features);
        when(contextService.technicalPlan(100L)).thenReturn(techPlan);
        when(contextService.marketingPlan(100L)).thenReturn(marketingPlan);
        when(contextService.budget(100L)).thenReturn(budget);
        when(contextService.roadmap(100L)).thenReturn(roadmap);
        when(decisionService.list(100L)).thenReturn(decisions);
    }

    @Test
    void build_usesAllFourDepartmentAnalyses() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.executiveSummarySection().source()).contains("CEO");
        assertThat(blueprint.technicalSection().source()).contains("Development");
        assertThat(blueprint.gtmSection().source()).contains("Marketing");
        assertThat(blueprint.financialSection().source()).contains("Finance");
    }

    @Test
    void build_reflectsCeoSynthesisAndFinalDecision() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.executiveSummarySection().pitch()).contains("SkinAI");
        assertThat(blueprint.executiveSummarySection().strategicObjectives()).contains("Ship focused V1 MVP");
        assertThat(blueprint.boardroomSection().ceoDecisionSummary()).contains("Defer Community Feed");
    }

    @Test
    void build_mvpFeaturesReflectFinalStateAndAreDisjointFromDeferred() {
        BlueprintDto blueprint = blueprintService.build(100L);

        List<String> mvpNames = blueprint.mvpSection().mvpFeatures().stream().map(MvpFeatureDetailDto::name).toList();
        List<String> deferredNames = blueprint.mvpSection().deferredFeatures().stream().map(MvpFeatureDetailDto::name).toList();

        assertThat(mvpNames).contains("AI Product Scanner", "Personalized Routine Generator");
        assertThat(mvpNames).doesNotContain("Community Skincare Feed");

        assertThat(deferredNames).contains("Community Skincare Feed");
        assertThat(deferredNames).doesNotContain("AI Product Scanner");
    }

    @Test
    void build_technicalPlanFromDeveloper() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.technicalSection().architecture()).isEqualTo(techPlan.getArchitecture());
        assertThat(blueprint.technicalSection().developmentTimeline()).isEqualTo(techPlan.getTimeline());
        assertThat(blueprint.technicalSection().estimatedEngineeringMonths()).isEqualTo(3.0);
    }

    @Test
    void build_marketingPlanFromMarketing() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.gtmSection().positioning()).isEqualTo(marketingPlan.getPositioning());
        assertThat(blueprint.gtmSection().launchStrategy()).isEqualTo(marketingPlan.getGoToMarket());
    }

    @Test
    void build_financePlanFromFinance() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.financialSection().runwayMonths()).isEqualTo(5.5);
        assertThat(blueprint.financialSection().totalUpfrontCost()).isEqualTo(50000.0);
    }

    @Test
    void build_includesBoardroomDecisions() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.boardroomSection().decisions()).hasSize(1);
        assertThat(blueprint.boardroomSection().decisions().get(0).decision()).contains("Defer Community Feed");
    }

    @Test
    void build_aggregatesRisksWithSource() {
        BlueprintDto blueprint = blueprintService.build(100L);

        List<String> descriptions = blueprint.risksSection().aggregatedRisks().stream().map(RiskItemDto::description).toList();
        assertThat(descriptions).contains("Scanner accuracy risk", "Model latency on mobile devices");
    }

    @Test
    void build_assumptionsRemainExplicitlyLabeled() {
        BlueprintDto blueprint = blueprintService.build(100L);

        List<String> statements = blueprint.assumptionsSection().aggregatedAssumptions().stream().map(AssumptionItemDto::statement).toList();
        assertThat(statements).allMatch(s -> s.startsWith("ASSUMPTION:"));
    }

    @Test
    void build_containsTraceabilityAndSourceInfo() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.whyThisPlanSection().reasoningChain()).hasSize(7);
        assertThat(blueprint.whyThisPlanSection().source()).contains("Boardroom Debate");
    }

    @Test
    void build_preservesExistingApiCompatibility() {
        BlueprintDto blueprint = blueprintService.build(100L);

        assertThat(blueprint.startupId()).isEqualTo(100L);
        assertThat(blueprint.name()).isEqualTo("SkinAI");
        assertThat(blueprint.pitch()).isNotBlank();
        assertThat(blueprint.mvp()).hasSize(2);
        assertThat(blueprint.backlog()).hasSize(1);
        assertThat(blueprint.complete()).isTrue();
    }
}
