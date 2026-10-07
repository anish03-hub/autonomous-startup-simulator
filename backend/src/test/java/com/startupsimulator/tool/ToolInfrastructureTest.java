package com.startupsimulator.tool;

import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.service.EventService;
import com.startupsimulator.tool.impl.DevelopmentEffortEstimatorTool;
import com.startupsimulator.tool.impl.FinancialCalculatorTool;
import com.startupsimulator.tool.impl.PricingRevenueCalculatorTool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Phase 4A tool-infrastructure tests (T1-T13 + the §18 architectural test).
 *
 * <p>Everything here is a plain unit test: no Spring context, no network, no LLM.
 * {@link EventService} is a Mockito mock so event emission can be asserted without
 * touching the database. The three real tools are exercised through the real
 * {@link ToolRegistry} and {@link ToolExecutionService} so the end-to-end
 * request → registry → service → tool → result chain is proven.
 */
class ToolInfrastructureTest {

    // ---- fixtures -----------------------------------------------------------

    private StartupContext context() {
        Startup s = new Startup();
        s.setId(1L);
        s.setName("Acme");
        s.setOriginalIdea("A deterministic widget business.");
        return new StartupContext(s);
    }

    private ToolRegistry fullRegistry() {
        return new ToolRegistry(List.of(
                new FinancialCalculatorTool(),
                new DevelopmentEffortEstimatorTool(),
                new PricingRevenueCalculatorTool()));
    }

    private ToolExecutionService service(ToolRegistry registry, EventService events) {
        return new ToolExecutionService(registry, events);
    }

    // ---- T1: registration ---------------------------------------------------

    @Test
    void t1_toolsRegisterAndResolveByName() {
        ToolRegistry registry = fullRegistry();

        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.contains("financial_calculator")).isTrue();
        assertThat(registry.contains("development_effort_estimator")).isTrue();
        assertThat(registry.contains("pricing_revenue_calculator")).isTrue();
        assertThat(registry.find("financial_calculator")).isPresent();
        assertThat(registry.find("nope")).isEmpty();
    }

    // ---- T2: duplicate registration fails clearly ---------------------------

    @Test
    void t2_duplicateRegistrationIsRefused() {
        ToolRegistry registry = fullRegistry();

        assertThatThrownBy(() -> registry.register(new FinancialCalculatorTool()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    // ---- T3: unknown tool → controlled FAILURE (no crash) -------------------

    @Test
    void t3_unknownToolYieldsControlledFailure() {
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(fullRegistry(), events);

        ToolResult result = exec.execute(
                ToolRequest.of(1L, AgentType.FINANCE.name(), "does_not_exist", ToolArguments.empty()),
                context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("UNKNOWN_TOOL");
        // Nothing executed → no event emitted.
        verifyNoInteractions(events);
    }

    // ---- T4: a valid request executes through the service -------------------

    @Test
    void t4_validRequestExecutesViaService() {
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(fullRegistry(), events);

        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", 50.0)
                .put("potentialCustomers", 10_000)
                .put("conversionRate", 0.05)
                .put("recurringPeriodsPerYear", 12)
                .build();

        ToolResult result = exec.execute(
                ToolRequest.of(1L, AgentType.MARKETING.name(), "pricing_revenue_calculator", args),
                context());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.result()).isInstanceOf(PricingRevenueCalculatorTool.Output.class);
    }

    // ---- T5: invalid arguments → controlled FAILURE -------------------------

    @Test
    void t5_missingArgumentYieldsControlledFailure() {
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(fullRegistry(), events);

        // financial_calculator requires several numbers; supply none.
        ToolResult result = exec.execute(
                ToolRequest.of(1L, AgentType.FINANCE.name(), "financial_calculator", ToolArguments.empty()),
                context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("MISSING_ARGUMENT");
    }

    @Test
    void t5_wrongTypeYieldsControlledFailure() {
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(fullRegistry(), events);

        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", "fifty") // not a number
                .put("potentialCustomers", 10)
                .put("conversionRate", 0.1)
                .build();

        ToolResult result = exec.execute(
                ToolRequest.of(1L, AgentType.MARKETING.name(), "pricing_revenue_calculator", args),
                context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("INVALID_TYPE");
    }

    // ---- T6: financial_calculator verifiable math ---------------------------

    @Test
    void t6_financialCalculatorMathIsExact() {
        FinancialCalculatorTool tool = new FinancialCalculatorTool();

        ToolArguments args = ToolArguments.builder()
                .put("developmentCost", 10_000.0)
                .put("infrastructureCost", 2_000.0)
                .put("marketingBudget", 3_000.0)
                .put("operatingCost", 1_000.0)
                .put("startingCapital", 100_000.0)
                .put("monthlyExpenses", 8_000.0)
                .put("monthlyRevenue", 3_000.0)
                .build();

        ToolResult result = tool.execute(
                ToolRequest.of(1L, AgentType.FINANCE.name(), tool.name(), args), context());

        assertThat(result.isSuccess()).isTrue();
        var out = (FinancialCalculatorTool.Output) result.result();
        assertThat(out.totalUpfrontCost()).isEqualTo(16_000.0);
        assertThat(out.capitalAfterUpfront()).isEqualTo(84_000.0);
        assertThat(out.monthlyBurn()).isEqualTo(5_000.0);
        assertThat(out.cashFlowPositive()).isFalse();
        assertThat(out.runwayMonths()).isEqualTo(16.8);
        assertThat(out.breakEvenMonths()).isNull();
    }

    @Test
    void t6_financialCalculatorBreakEvenWhenProfitable() {
        FinancialCalculatorTool tool = new FinancialCalculatorTool();

        ToolArguments args = ToolArguments.builder()
                .put("developmentCost", 16_000.0)
                .put("infrastructureCost", 0.0)
                .put("marketingBudget", 0.0)
                .put("operatingCost", 0.0)
                .put("startingCapital", 50_000.0)
                .put("monthlyExpenses", 8_000.0)
                .put("monthlyRevenue", 12_000.0)
                .build();

        var out = (FinancialCalculatorTool.Output) tool.execute(
                ToolRequest.of(1L, AgentType.FINANCE.name(), tool.name(), args), context()).result();

        assertThat(out.cashFlowPositive()).isTrue();
        assertThat(out.runwayMonths()).isNull();          // not burning
        assertThat(out.breakEvenMonths()).isEqualTo(4.0); // 16000 / (12000-8000)
    }

    // ---- T7: development_effort_estimator expected output -------------------

    @Test
    void t7_developmentEffortMathIsExact() {
        DevelopmentEffortEstimatorTool tool = new DevelopmentEffortEstimatorTool();

        ToolArguments args = ToolArguments.builder()
                .put("featureEfforts", List.of(3, 4, 2, 5))
                .put("engineerCount", 2)
                .put("hoursPerPoint", 8.0)
                .build();

        var out = (DevelopmentEffortEstimatorTool.Output) tool.execute(
                ToolRequest.of(1L, AgentType.DEVELOPMENT.name(), tool.name(), args), context()).result();

        assertThat(out.featureCount()).isEqualTo(4);
        assertThat(out.totalEffortPoints()).isEqualTo(14);
        assertThat(out.estimatedPersonHours()).isEqualTo(112.0);
        assertThat(out.estimatedCalendarDays()).isEqualTo(7.0); // 112 / (2 * 8)
        assertThat(out.averageEffortPerFeature()).isEqualTo(3.5);
    }

    // ---- T8: pricing_revenue_calculator expected output ---------------------

    @Test
    void t8_pricingRevenueMathIsExact() {
        PricingRevenueCalculatorTool tool = new PricingRevenueCalculatorTool();

        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", 50.0)
                .put("potentialCustomers", 10_000)
                .put("conversionRate", 0.05)
                .put("recurringPeriodsPerYear", 12)
                .build();

        var out = (PricingRevenueCalculatorTool.Output) tool.execute(
                ToolRequest.of(1L, AgentType.MARKETING.name(), tool.name(), args), context()).result();

        assertThat(out.payingCustomers()).isEqualTo(500L);
        assertThat(out.revenuePerPeriod()).isEqualTo(25_000.0);
        assertThat(out.annualRevenue()).isEqualTo(300_000.0);
        assertThat(out.recurringPeriodsPerYear()).isEqualTo(12);
    }

    // ---- T9: SUCCESS / FAILURE are distinguishable --------------------------

    @Test
    void t9_successAndFailureAreDistinguishable() {
        ToolResult ok = ToolResult.success("t", "payload", "r1", 3L);
        ToolResult bad = ToolResult.failure("t", "CODE", "safe message", "r2", 1L);

        assertThat(ok.isSuccess()).isTrue();
        assertThat(ok.isFailure()).isFalse();
        assertThat(ok.status()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(ok.result()).isEqualTo("payload");
        assertThat(ok.errorCode()).isNull();

        assertThat(bad.isFailure()).isTrue();
        assertThat(bad.isSuccess()).isFalse();
        assertThat(bad.status()).isEqualTo(ToolStatus.FAILURE);
        assertThat(bad.result()).isNull();
        assertThat(bad.errorCode()).isEqualTo("CODE");
        assertThat(bad.errorMessage()).isEqualTo("safe message");
    }

    // ---- T10: a tool that throws is contained (no crash escapes) ------------

    /** A deliberately misbehaving tool used to prove failures are contained. */
    private static final class ExplodingTool extends AbstractTool {
        @Override public String name() { return "exploding_tool"; }
        @Override public String description() { return "Always throws."; }
        @Override public ToolSchema schema() { return new ToolSchema(name(), description(), List.of()); }
        @Override protected Object run(ToolArguments args, StartupContext context) {
            throw new IllegalStateException("boom — this should never reach the agent");
        }
    }

    @Test
    void t10_throwingToolIsContainedAsControlledFailure() {
        ToolRegistry registry = new ToolRegistry(List.of(new ExplodingTool()));
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(registry, events);

        ToolResult result = exec.execute(
                ToolRequest.of(1L, AgentType.CEO.name(), "exploding_tool", ToolArguments.empty()),
                context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("EXECUTION_ERROR");
        // The raw exception text/stack trace is NOT leaked to the agent.
        assertThat(result.errorMessage()).doesNotContain("boom");
        assertThat(result.errorMessage()).doesNotContainIgnoringCase("exception");
    }

    // ---- T11: structured metadata (names / descriptions / input schema) -----

    @Test
    void t11_registryExposesStructuredMetadata() {
        List<ToolSchema> schemas = fullRegistry().listAvailable();

        assertThat(schemas).hasSize(3);
        assertThat(schemas).extracting(ToolSchema::toolName)
                .containsExactlyInAnyOrder(
                        "financial_calculator", "development_effort_estimator", "pricing_revenue_calculator");
        assertThat(schemas).allSatisfy(s -> {
            assertThat(s.description()).isNotBlank();
            assertThat(s.parameters()).isNotEmpty();
            assertThat(s.parameters()).allSatisfy(p -> {
                assertThat(p.name()).isNotBlank();
                assertThat(p.type()).isNotNull();
            });
        });

        ToolSchema pricing = schemas.stream()
                .filter(s -> s.toolName().equals("pricing_revenue_calculator")).findFirst().orElseThrow();
        assertThat(pricing.parameters()).extracting(ToolParameter::name)
                .contains("pricePerUnit", "potentialCustomers", "conversionRate");
    }

    // ---- T12: event behaviour (real executions only) ------------------------

    @Test
    void t12_successEmitsStartedThenSucceeded() {
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(fullRegistry(), events);

        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", 10.0).put("potentialCustomers", 100).put("conversionRate", 0.1).build();
        exec.execute(ToolRequest.of(1L, AgentType.MARKETING.name(), "pricing_revenue_calculator", args), context());

        ArgumentCaptor<EventType> types = ArgumentCaptor.forClass(EventType.class);
        verify(events, org.mockito.Mockito.times(2))
                .record(org.mockito.Mockito.eq(1L), types.capture(), org.mockito.Mockito.anyString(),
                        org.mockito.Mockito.<Map<String, Object>>any());
        assertThat(types.getAllValues())
                .containsExactly(EventType.TOOL_EXECUTION_STARTED, EventType.TOOL_EXECUTION_SUCCEEDED);
    }

    @Test
    void t12_toolFailureEmitsStartedThenFailed() {
        ToolRegistry registry = new ToolRegistry(List.of(new ExplodingTool()));
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(registry, events);

        exec.execute(ToolRequest.of(1L, AgentType.CEO.name(), "exploding_tool", ToolArguments.empty()), context());

        ArgumentCaptor<EventType> types = ArgumentCaptor.forClass(EventType.class);
        verify(events, org.mockito.Mockito.times(2))
                .record(org.mockito.Mockito.eq(1L), types.capture(), org.mockito.Mockito.anyString(),
                        org.mockito.Mockito.<Map<String, Object>>any());
        assertThat(types.getAllValues())
                .containsExactly(EventType.TOOL_EXECUTION_STARTED, EventType.TOOL_EXECUTION_FAILED);
    }

    @Test
    void t12_noEventWhenNothingExecuted() {
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(fullRegistry(), events);

        // Unknown tool and a null request both resolve before any tool runs.
        exec.execute(ToolRequest.of(1L, AgentType.CEO.name(), "ghost", ToolArguments.empty()), context());
        exec.execute(null, context());

        verifyNoInteractions(events);
    }

    // ---- T13: bounds / extreme input → controlled, never uncontrolled -------

    @Test
    void t13_nonFiniteArgumentIsRejected() {
        PricingRevenueCalculatorTool tool = new PricingRevenueCalculatorTool();
        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", Double.NaN)
                .put("potentialCustomers", 10)
                .put("conversionRate", 0.1)
                .build();

        ToolResult result = tool.execute(ToolRequest.of(1L, "x", tool.name(), args), context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("INVALID_NUMBER");
    }

    @Test
    void t13_outOfRangeArgumentIsRejected() {
        PricingRevenueCalculatorTool tool = new PricingRevenueCalculatorTool();
        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", 10.0)
                .put("potentialCustomers", 10)
                .put("conversionRate", 5.0) // must be in [0, 1]
                .build();

        ToolResult result = tool.execute(ToolRequest.of(1L, "x", tool.name(), args), context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("OUT_OF_RANGE");
    }

    @Test
    void t13_divideByZeroGuardOnEmptyFeatureList() {
        DevelopmentEffortEstimatorTool tool = new DevelopmentEffortEstimatorTool();
        ToolArguments args = ToolArguments.builder().put("featureEfforts", List.of()).build();

        ToolResult result = tool.execute(ToolRequest.of(1L, "x", tool.name(), args), context());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("OUT_OF_RANGE");
    }

    @Test
    void t13_extremeButFiniteInputComputesWithoutCrashing() {
        PricingRevenueCalculatorTool tool = new PricingRevenueCalculatorTool();
        ToolArguments args = ToolArguments.builder()
                .put("pricePerUnit", 1_000_000.0)
                .put("potentialCustomers", 1_000_000_000)
                .put("conversionRate", 1.0)
                .put("recurringPeriodsPerYear", 365)
                .build();

        ToolResult result = tool.execute(ToolRequest.of(1L, "x", tool.name(), args), context());

        assertThat(result.isSuccess()).isTrue();
        var out = (PricingRevenueCalculatorTool.Output) result.result();
        assertThat(out.payingCustomers()).isEqualTo(1_000_000_000L);
        assertThat(Double.isFinite(out.annualRevenue())).isTrue();
    }

    @Test
    void t13_nullRequestIsControlledFailure() {
        ToolResult result = service(fullRegistry(), mock(EventService.class)).execute(null, context());
        assertThat(result.isFailure()).isTrue();
        assertThat(result.errorCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void t13_missingStartupOrAgentIsControlledFailure() {
        ToolExecutionService exec = service(fullRegistry(), mock(EventService.class));

        ToolResult noStartup = exec.execute(
                new ToolRequest(null, null, "FINANCE", "financial_calculator", ToolArguments.empty()), context());
        ToolResult noAgent = exec.execute(
                new ToolRequest(null, 1L, "  ", "financial_calculator", ToolArguments.empty()), context());

        assertThat(noStartup.errorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(noAgent.errorCode()).isEqualTo("INVALID_REQUEST");
    }

    // ---- §18: end-to-end architectural test ---------------------------------
    // Proves ToolRequest → ToolRegistry → ToolExecutionService → real Tool →
    // real calculation → ToolResult, with the registry AND the execution service
    // genuinely participating (the test never calls the tool directly).

    @Test
    void arch_requestFlowsThroughRegistryAndServiceToRealTool() {
        // Real collaborators, wired exactly as Spring would wire them.
        ToolRegistry registry = fullRegistry();
        EventService events = mock(EventService.class);
        ToolExecutionService exec = service(registry, events);

        // The registry must actually be consulted: an unregistered name fails here.
        assertThat(registry.find("financial_calculator")).isPresent();

        ToolArguments args = ToolArguments.builder()
                .put("developmentCost", 20_000.0).put("infrastructureCost", 0.0)
                .put("marketingBudget", 0.0).put("operatingCost", 0.0)
                .put("startingCapital", 60_000.0)
                .put("monthlyExpenses", 10_000.0).put("monthlyRevenue", 0.0)
                .build();
        ToolRequest request = ToolRequest.of(1L, AgentType.FINANCE.name(), "financial_calculator", args);

        // Only ever call the SERVICE — never the tool directly.
        ToolResult result = exec.execute(request, context());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.toolName()).isEqualTo("financial_calculator");
        assertThat(result.requestId()).isEqualTo(request.requestId());
        var out = (FinancialCalculatorTool.Output) result.result();
        assertThat(out.totalUpfrontCost()).isEqualTo(20_000.0);
        assertThat(out.runwayMonths()).isEqualTo(4.0); // (60000 - 20000) / 10000

        // The service genuinely ran the tool → a STARTED and a SUCCEEDED event.
        verify(events).record(org.mockito.Mockito.eq(1L),
                org.mockito.Mockito.eq(EventType.TOOL_EXECUTION_STARTED),
                org.mockito.Mockito.anyString(), org.mockito.Mockito.<Map<String, Object>>any());
        verify(events).record(org.mockito.Mockito.eq(1L),
                org.mockito.Mockito.eq(EventType.TOOL_EXECUTION_SUCCEEDED),
                org.mockito.Mockito.anyString(), org.mockito.Mockito.<Map<String, Object>>any());
        verify(events, never()).record(org.mockito.Mockito.anyLong(),
                org.mockito.Mockito.eq(EventType.TOOL_EXECUTION_FAILED),
                org.mockito.Mockito.anyString(), org.mockito.Mockito.<Map<String, Object>>any());
    }
}
