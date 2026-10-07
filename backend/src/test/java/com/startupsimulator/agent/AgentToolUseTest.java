package com.startupsimulator.agent;

import com.startupsimulator.config.AiExecutionMode;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.service.EventService;
import com.startupsimulator.tool.AbstractTool;
import com.startupsimulator.tool.ToolArguments;
import com.startupsimulator.tool.ToolExecutionService;
import com.startupsimulator.tool.ToolRegistry;
import com.startupsimulator.tool.ToolSchema;
import com.startupsimulator.tool.impl.DevelopmentEffortEstimatorTool;
import com.startupsimulator.tool.impl.FinancialCalculatorTool;
import com.startupsimulator.tool.impl.PricingRevenueCalculatorTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Phase 4B — genuine, LLM-driven tool use. Proves the complete runtime chain:
 * FAKE/REAL LLM returns a structured USE_TOOL step → {@link AgentToolReasoner}
 * builds a {@link com.startupsimulator.tool.ToolRequest} from the model's own
 * choice → {@link ToolExecutionService} runs the ACTUAL tool → the real
 * {@link com.startupsimulator.tool.ToolResult} is injected into the agent's NEXT
 * prompt → the LLM reasons again and returns FINAL.
 *
 * <p>Java never selects, substitutes, or overrides a tool; the §20 negative test
 * proves selection is agent-driven. Everything runs offline through
 * {@link FakeLLMService} — no network, no real provider.
 */
class AgentToolUseTest {

    private ToolRegistry registry;
    private ToolExecutionService exec;
    private AgentToolReasoner reasoner;
    private StartupContext ctx;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry(List.of(
                new FinancialCalculatorTool(),
                new DevelopmentEffortEstimatorTool(),
                new PricingRevenueCalculatorTool()));
        exec = new ToolExecutionService(registry, mock(EventService.class));
        reasoner = new AgentToolReasoner(registry, exec);
        ctx = newContext();
    }

    // ---- helpers ------------------------------------------------------------

    private static StartupContext newContext() {
        Startup s = new Startup();
        s.setId(1L);
        s.setName("Acme");
        s.setOriginalIdea("A SaaS tool for small bakeries.");
        return new StartupContext(s);
    }

    private static LlmProperties realProps() {
        LlmProperties p = new LlmProperties();
        p.setMode(AiExecutionMode.REAL);
        return p;
    }

    /** financial_calculator inputs → a distinctive totalUpfrontCost of 18500.0. */
    private static Map<String, Object> financialArgs() {
        return Map.of(
                "developmentCost", 10000, "infrastructureCost", 2000,
                "marketingBudget", 5000, "operatingCost", 1500,
                "startingCapital", 60000, "monthlyExpenses", 4000);
    }

    /** development_effort_estimator inputs → distinctive estimatedPersonHours 80.0. */
    private static Map<String, Object> effortArgs() {
        return Map.of("featureEfforts", List.of(3, 5, 2));
    }

    /** pricing_revenue_calculator inputs → distinctive annualRevenue 54000.0. */
    private static Map<String, Object> pricingArgs() {
        return Map.of("pricePerUnit", 9.0, "potentialCustomers", 10000,
                "conversionRate", 0.05, "recurringPeriodsPerYear", 12);
    }

    /** A tool that always throws, to exercise the EXECUTION_ERROR failure path. */
    static final class BoomTool extends AbstractTool {
        @Override public String name() { return "boom_tool"; }
        @Override public String description() { return "Always throws, for testing."; }
        @Override public ToolSchema schema() { return new ToolSchema(name(), description(), List.of()); }
        @Override protected Object run(ToolArguments args, StartupContext context) {
            throw new RuntimeException("boom");
        }
    }

    // ---- T1: the tool catalogue is generated FROM the registry into the prompt
    @Test
    void t1_toolCatalogue_isGeneratedFromRegistry_intoThePrompt() {
        FakeLLMService fake = FakeLLMService.returningSequence(AgentToolStep.finalAnswer("done"));
        reasoner.reason(fake, true, "persona", "=== STARTUP CONTEXT ===\n", "FINANCE", 1L, ctx);

        assertThat(fake.lastSystemPrompt).contains("=== TOOL USE PROTOCOL ===");
        assertThat(fake.userPrompts.get(0))
                .contains("=== AVAILABLE TOOLS ===")
                .contains("financial_calculator")
                .contains("development_effort_estimator")
                .contains("pricing_revenue_calculator");
    }

    // ---- T2: the LLM-chosen tool name + args are parsed and executed (not Java's)
    @Test
    void t2_llmSelectedTool_isParsedAndExecuted() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("development_effort_estimator", "size it", effortArgs()),
                AgentToolStep.finalAnswer("done"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "DEVELOPMENT", 1L, ctx);

        assertThat(out.toolExecutions()).isEqualTo(1);
        ToolInteraction it = out.interactions().get(0);
        assertThat(it.toolName()).isEqualTo("development_effort_estimator");
        assertThat(it.arguments()).containsKey("featureEfforts");
    }

    // ---- T3: the ACTUAL tool runs and the real ToolResult comes back
    @Test
    void t3_actualTool_executes_realResultReturned() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "cash maths", financialArgs()),
                AgentToolStep.finalAnswer("done"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        ToolInteraction it = out.interactions().get(0);
        assertThat(it.succeeded()).isTrue();
        FinancialCalculatorTool.Output o = (FinancialCalculatorTool.Output) it.result().result();
        assertThat(o.totalUpfrontCost()).isEqualTo(18500.0);   // 10000+2000+5000+1500
        assertThat(o.capitalAfterUpfront()).isEqualTo(41500.0); // 60000-18500
    }

    // ---- T4: the real tool result is injected into the agent's NEXT prompt
    @Test
    void t4_toolResult_entersTheNextPrompt() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "cash maths", financialArgs()),
                AgentToolStep.finalAnswer("done"));

        reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(fake.structuredCalls).isEqualTo(2);
        assertThat(fake.userPrompts.get(0)).doesNotContain("18500.0");   // not yet computed
        assertThat(fake.userPrompts.get(1))
                .contains("=== TOOL INTERACTION HISTORY ===")
                .contains("SUCCESS")
                .contains("18500.0");                                     // the real figure
    }

    // ---- T5: the LLM's FINAL is produced AFTER it has seen the tool result
    @Test
    void t5_llmFinalResponse_comesAfterSeeingResult() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "cash maths", financialArgs()),
                AgentToolStep.finalAnswer("Runway looks healthy."));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
        assertThat(out.finalResponse()).isEqualTo("Runway looks healthy.");
        assertThat(out.failed()).isFalse();
    }

    // ---- T6: multiple bounded tool calls before FINAL
    @Test
    void t6_multipleToolCalls_bounded_thenFinal() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "a", financialArgs()),
                AgentToolStep.useTool("pricing_revenue_calculator", "b", pricingArgs()),
                AgentToolStep.finalAnswer("done"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.toolExecutions()).isEqualTo(2);
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
    }

    // ---- T7: the tool-call budget is enforced — never an unbounded loop
    @Test
    void t7_toolCallLimit_isEnforced_noUnboundedLoop() {
        // The model keeps asking for tools and never finalises; the drained
        // sequence reuses the last (USE_TOOL) response, so without a cap this
        // would loop forever.
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "again", financialArgs()));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.toolExecutions()).isEqualTo(AgentToolReasoner.MAX_TOOL_CALLS); // exactly 3
        assertThat(fake.structuredCalls).isEqualTo(AgentToolReasoner.MAX_TOOL_CALLS + 1); // + forced finalise
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.BUDGET_EXHAUSTED);
    }

    // ---- T8: an unknown tool name is a controlled failure fed back to the LLM
    @Test
    void t8_unknownTool_isControlledFailure_visibleToLlm() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("nonexistent_tool", "oops", Map.of()),
                AgentToolStep.finalAnswer("recovered"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        ToolInteraction it = out.interactions().get(0);
        assertThat(it.result().isFailure()).isTrue();
        assertThat(it.result().errorCode()).isEqualTo("UNKNOWN_TOOL");
        assertThat(fake.userPrompts.get(1)).contains("FAILURE").contains("UNKNOWN_TOOL");
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL); // no crash
    }

    // ---- T9: invalid arguments → FAILURE surfaced to the LLM, no crash
    @Test
    void t9_invalidArguments_areFailureVisibleToLlm() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                // financial_calculator requires several args; supply only one.
                AgentToolStep.useTool("financial_calculator", "bad", Map.of("developmentCost", 10000)),
                AgentToolStep.finalAnswer("recovered"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        ToolInteraction it = out.interactions().get(0);
        assertThat(it.result().isFailure()).isTrue();
        assertThat(it.result().errorCode()).isEqualTo("MISSING_ARGUMENT");
        assertThat(fake.userPrompts.get(1)).contains("FAILURE");
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
    }

    // ---- T10: a tool that throws internally → controlled EXECUTION_ERROR
    @Test
    void t10_toolInternalException_isControlledFailure() {
        ToolRegistry boomRegistry = new ToolRegistry(List.of(new BoomTool()));
        AgentToolReasoner boomReasoner = new AgentToolReasoner(boomRegistry,
                new ToolExecutionService(boomRegistry, mock(EventService.class)));
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("boom_tool", "run it", Map.of()),
                AgentToolStep.finalAnswer("recovered"));

        ToolAssistedOutcome out = boomReasoner.reason(fake, true, "persona", "ctx", "DEVELOPMENT", 1L, ctx);

        assertThat(out.interactions().get(0).result().errorCode()).isEqualTo("EXECUTION_ERROR");
        assertThat(fake.userPrompts.get(1)).contains("EXECUTION_ERROR");
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
    }

    // ---- T11 / §20 NEGATIVE: the agent runs the tool the LLM chose, not the
    // "expected" one for its department. Finance is handed pricing_revenue_calculator
    // (NOT financial_calculator); the infrastructure must execute what was selected.
    @Test
    void t11_financeAgent_executesLlmSelectedTool_notTheExpectedOne() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("pricing_revenue_calculator", "model revenue", pricingArgs()),
                AgentToolStep.finalAnswer("done"));
        FinanceAgent finance = new FinanceAgent(fake, realProps());

        ToolAssistedOutcome out = finance.reasonWithTools(ctx, reasoner);

        ToolInteraction it = out.interactions().get(0);
        assertThat(it.toolName()).isEqualTo("pricing_revenue_calculator");
        assertThat(it.toolName()).isNotEqualTo("financial_calculator"); // Java did NOT substitute
        assertThat(it.succeeded()).isTrue();
    }

    // ---- T12: Developer agent is genuinely tool-capable
    @Test
    void t12_developerAgent_toolFlow() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("development_effort_estimator", "estimate", effortArgs()),
                AgentToolStep.finalAnswer("Plan is ~80 person-hours."));
        DeveloperAgent dev = new DeveloperAgent(fake, realProps());

        ToolAssistedOutcome out = dev.reasonWithTools(ctx, reasoner);

        assertThat(out.usedTool()).isTrue();
        assertThat(out.interactions().get(0).toolName()).isEqualTo("development_effort_estimator");
        assertThat(fake.userPrompts.get(1)).contains("80.0"); // 10 points * 8 hrs
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
    }

    // ---- T13: Marketing agent is genuinely tool-capable
    @Test
    void t13_marketingAgent_toolFlow() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("pricing_revenue_calculator", "price it", pricingArgs()),
                AgentToolStep.finalAnswer("Annual revenue ~54k."));
        MarketingAgent marketing = new MarketingAgent(fake, realProps());

        ToolAssistedOutcome out = marketing.reasonWithTools(ctx, reasoner);

        assertThat(out.interactions().get(0).toolName()).isEqualTo("pricing_revenue_calculator");
        assertThat(fake.userPrompts.get(1)).contains("54000.0"); // 500 * 9 * 12
    }

    // ---- T14: Finance agent is genuinely tool-capable (its "expected" tool)
    @Test
    void t14_financeAgent_toolFlow() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "cash maths", financialArgs()),
                AgentToolStep.finalAnswer("Upfront is 18.5k."));
        FinanceAgent finance = new FinanceAgent(fake, realProps());

        ToolAssistedOutcome out = finance.reasonWithTools(ctx, reasoner);

        assertThat(out.interactions().get(0).toolName()).isEqualTo("financial_calculator");
        assertThat(fake.userPrompts.get(1)).contains("18500.0");
    }

    // ---- T15: REAL mode — the tool selection comes from the structured response
    @Test
    void t15_realMode_selectionComesFromStructuredResponse() {
        // Two different structured responses select two different tools through the
        // SAME code path: proof the choice is data-driven, not agent-type routing.
        FinanceAgent financePicksDev = new FinanceAgent(
                FakeLLMService.returningSequence(
                        AgentToolStep.useTool("development_effort_estimator", "x", effortArgs()),
                        AgentToolStep.finalAnswer("done")),
                realProps());
        DeveloperAgent devPicksFinance = new DeveloperAgent(
                FakeLLMService.returningSequence(
                        AgentToolStep.useTool("financial_calculator", "x", financialArgs()),
                        AgentToolStep.finalAnswer("done")),
                realProps());

        assertThat(financePicksDev.reasonWithTools(ctx, reasoner).interactions().get(0).toolName())
                .isEqualTo("development_effort_estimator");
        assertThat(devPicksFinance.reasonWithTools(ctx, reasoner).interactions().get(0).toolName())
                .isEqualTo("financial_calculator");
    }

    // ---- T16: SCRIPTED_DEMO — no real LLM call, nothing fabricated
    @Test
    void t16_scriptedDemo_makesNoLlmCall_andFabricatesNothing() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "x", financialArgs()),
                AgentToolStep.finalAnswer("done"));
        FinanceAgent finance = new FinanceAgent(fake, new LlmProperties()); // default SCRIPTED_DEMO

        ToolAssistedOutcome out = finance.reasonWithTools(ctx, reasoner);

        assertThat(out.usedRealLlm()).isFalse();
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.NO_REAL_PROVIDER);
        assertThat(out.usedTool()).isFalse();
        assertThat(fake.structuredCalls).isZero(); // ZERO LLM calls
    }

    // ---- T17: the tool result genuinely changes the next prompt (distinct figure)
    @Test
    void t17_toolResult_changesTheNextPrompt() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("pricing_revenue_calculator", "x", pricingArgs()),
                AgentToolStep.finalAnswer("done"));

        reasoner.reason(fake, true, "persona", "ctx", "MARKETING", 1L, ctx);

        assertThat(fake.userPrompts.get(0)).doesNotContain("54000.0");
        assertThat(fake.userPrompts.get(1)).contains("54000.0"); // annualRevenue appeared
    }

    // ---- T18 / §15: Phase 3 messaging is preserved — the inbox reaches the prompt
    @Test
    void t18_phase3Messaging_isPreserved_inToolPrompt() {
        ctx.deliverInbox(AgentType.FINANCE, List.of(
                new AgentMessage(1L, AgentType.DEVELOPMENT, AgentType.FINANCE,
                        "Infra costs", "Expect 2k/mo in cloud spend.")));
        FakeLLMService fake = FakeLLMService.returningSequence(AgentToolStep.finalAnswer("noted"));
        FinanceAgent finance = new FinanceAgent(fake, realProps());

        finance.reasonWithTools(ctx, reasoner);

        assertThat(fake.userPrompts.get(0))
                .contains("MESSAGES ADDRESSED TO YOU")
                .contains("Expect 2k/mo in cloud spend."); // the Phase 3 message survived
    }

    // ---- §19 MOST IMPORTANT: the complete agent-driven tool chain, end to end.
    // Fails if Java hard-codes the tool, the tool is not executed, the result is
    // not returned, the result is not in the next prompt, or FINAL precedes it.
    @Test
    void e2e_llmDrivenToolChain_producesFinalAfterRealToolResult() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "need runway", financialArgs()),
                AgentToolStep.finalAnswer("With 18.5k upfront, runway is comfortable."));
        FinanceAgent finance = new FinanceAgent(fake, realProps());

        ToolAssistedOutcome out = finance.reasonWithTools(ctx, reasoner);

        // 1. the model made exactly two reasoning calls: decide-tool, then finalise
        assertThat(fake.structuredCalls).isEqualTo(2);
        // 2. the tool the LLM named was actually executed and succeeded
        ToolInteraction it = out.interactions().get(0);
        assertThat(it.toolName()).isEqualTo("financial_calculator");
        assertThat(it.succeeded()).isTrue();
        // 3. the ACTUAL computed result (not fabricated) came back
        FinancialCalculatorTool.Output o = (FinancialCalculatorTool.Output) it.result().result();
        assertThat(o.totalUpfrontCost()).isEqualTo(18500.0);
        // 4. the first prompt had the catalogue but NOT the result; the second does
        assertThat(fake.userPrompts.get(0)).contains("=== AVAILABLE TOOLS ===").doesNotContain("18500.0");
        assertThat(fake.userPrompts.get(1)).contains("18500.0");
        // 5. the FINAL answer was produced AFTER the result, by the LLM
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
        assertThat(out.finalResponse()).isEqualTo("With 18.5k upfront, runway is comfortable.");
    }

    // ---- §22: a malformed step is fed back as a note and the model recovers
    @Test
    void malformedStep_isFedBack_andModelRecovers() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                new AgentToolStep(null, null, null, null, null), // unknown action
                AgentToolStep.finalAnswer("recovered"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.interactions()).hasSize(1);
        assertThat(out.interactions().get(0).isMalformed()).isTrue();
        assertThat(fake.userPrompts.get(1)).contains("INVALID");
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
    }

    // ---- §22: a USE_TOOL step with an empty tool name is a controlled note
    @Test
    void useToolWithBlankName_isControlledNote_noExecution() {
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("  ", "blank", Map.of()),
                AgentToolStep.finalAnswer("recovered"));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.toolExecutions()).isZero(); // nothing executed
        assertThat(out.interactions().get(0).isMalformed()).isTrue();
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.FINAL);
    }

    // ---- §12: an LLM/transport failure stays an explicit failure (no fabrication)
    @Test
    void llmTransportFailure_isExplicitFailure_notFabricated() {
        FakeLLMService fake = FakeLLMService.returningSequence(FakeLLMService.FAILURE);

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.failed()).isTrue();
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.LLM_FAILURE);
        assertThat(out.finalResponse()).isNull(); // nothing invented
    }

    // ---- §5/§22: a USE_TOOL requested past the budget is NOT executed
    @Test
    void useToolRequestedPastBudget_isNotExecuted() {
        // 3 real executions, then a 4th USE_TOOL that must be refused (not run).
        FakeLLMService fake = FakeLLMService.returningSequence(
                AgentToolStep.useTool("financial_calculator", "1", financialArgs()),
                AgentToolStep.useTool("financial_calculator", "2", financialArgs()),
                AgentToolStep.useTool("financial_calculator", "3", financialArgs()),
                AgentToolStep.useTool("financial_calculator", "4", financialArgs()));

        ToolAssistedOutcome out = reasoner.reason(fake, true, "persona", "ctx", "FINANCE", 1L, ctx);

        assertThat(out.toolExecutions()).isEqualTo(AgentToolReasoner.MAX_TOOL_CALLS); // never 4
        assertThat(out.terminationReason()).isEqualTo(ToolAssistedOutcome.BUDGET_EXHAUSTED);
    }
}
