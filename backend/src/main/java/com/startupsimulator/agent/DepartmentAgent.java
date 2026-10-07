package com.startupsimulator.agent;

import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.enums.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Shared base for the real, LLM-backed department agents (Development,
 * Marketing, Finance). It mirrors the {@link CeoAgent} pattern so all four
 * agents reason through the single {@link LLMService} seam — never a second LLM
 * client, never a direct provider SDK call.
 *
 * <p>The template method {@link #runAnalysis(StartupContext)} implements the
 * real / fallback / mock branching once:
 * <ul>
 *   <li><b>real provider</b> — call {@link #applyRealAnalysis} (which does
 *       {@code generateStructured} → validate → normalise → write to state);
 *       on {@link AgentAnalysisException} or {@link CeoAnalysisException} fall
 *       back to {@link #applyDeterministicAnalysis} and flag the failure.</li>
 *   <li><b>offline/mock</b> — deterministic analysis, exactly the Phase 1
 *       behaviour, with no LLM call.</li>
 * </ul>
 *
 * <p>Each simulation therefore makes <b>exactly one</b> structured LLM call per
 * department when a real provider is configured. {@link #say(String)} phrasing
 * stays an identity passthrough (cost-free) in every provider.
 *
 * <p><b>Building on previous output:</b> {@link #ceoContextBlock(StartupContext)}
 * injects the CEO's structured analysis into every department's user prompt, and
 * because the orchestrator runs the departments in order (CEO → Developer →
 * Marketing → Finance) each later department also sees the accumulated
 * {@link StartupContext} the earlier ones filled in.
 */
public abstract class DepartmentAgent extends AbstractStartupAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final String systemPrompt;
    protected final LlmProperties llmProperties;

    protected DepartmentAgent(LLMService llm, LlmProperties llmProperties, String systemPromptPath) {
        super(llm);
        this.llmProperties = llmProperties;
        this.systemPrompt = loadPrompt(systemPromptPath);
    }

    /** Whether this agent will attempt a real LLM call (a provider with credentials). */
    public boolean usesRealProvider() {
        return llm.isRealProvider();
    }

    /**
     * Whether a genuine LLM reasoning path will actually run: the operator asked
     * for {@link com.startupsimulator.config.AiExecutionMode#REAL} <em>and</em> a
     * real provider is available. This — not {@link #usesRealProvider()} alone —
     * is the authoritative-output gate.
     */
    public boolean usesRealLlm() {
        return llmProperties.isRealMode() && llm.isRealProvider();
    }

    /** Active provider identifier, e.g. "openai" or "mock". */
    public String providerName() {
        return llm.provider();
    }

    // ---- Template method ----------------------------------------------------

    /**
     * Run this department's analysis with full metadata. Never throws. The mode
     * (not merely provider availability) decides the path:
     * <ul>
     *   <li><b>REAL + provider available</b> — the LLM output is validated and
     *       written to state as authoritative. On invalid/failed output the run
     *       records an <em>explicit failure</em> (provider {@code "failed"}) and
     *       applies <b>no</b> deterministic fabrication.</li>
     *   <li><b>REAL + no real provider</b> — explicit failure (provider
     *       {@code "unavailable"}); nothing is fabricated.</li>
     *   <li><b>SCRIPTED_DEMO</b> — the deterministic Phase 1 analysis, with
     *       <b>zero</b> LLM calls (provider {@code "mock"}).</li>
     * </ul>
     * The returned outcome drives event emission.
     */
    public AgentAnalysisOutcome runAnalysis(StartupContext ctx) {
        if (llmProperties.isRealMode()) {
            if (!llm.isRealProvider()) {
                // REAL requested but no real provider — fail loudly, never fabricate.
                String msg = "REAL mode requires a configured LLM provider, but '" + llm.provider()
                        + "' cannot reach a model. No " + type().getDisplayName()
                        + " analysis was produced.";
                log.warn("{} analysis skipped: {}", type(), msg);
                recordAnalysisMeta(ctx, "unavailable", true, msg);
                return AgentAnalysisOutcome.failed(type(), "unavailable", msg,
                        type().getDisplayName() + " analysis skipped: no real LLM provider is available in REAL mode.");
            }
            try {
                log.info("{} analysis starting via real provider '{}' for startup {}",
                        type(), llm.provider(), ctx.startupId());
                String headline = applyRealAnalysis(ctx);
                recordAnalysisMeta(ctx, llm.provider(), false, null);
                log.info("{} analysis completed via '{}' for startup {}",
                        type(), llm.provider(), ctx.startupId());
                return AgentAnalysisOutcome.success(type(), llm.provider(), headline);
            } catch (AgentAnalysisException | CeoAnalysisException e) {
                // REAL mode: invalid/failed LLM output is an explicit failure. We do
                // NOT run the deterministic script — fabricating reasoning would
                // defeat the whole point of REAL mode.
                String err = shortError(e);
                log.warn("{} REAL analysis failed for startup {} ({}); recording explicit failure (no fabrication).",
                        type(), ctx.startupId(), err);
                recordAnalysisMeta(ctx, "failed", true, err);
                return AgentAnalysisOutcome.failed(type(), "failed", err,
                        type().getDisplayName() + " analysis failed: " + err);
            }
        }
        // SCRIPTED_DEMO path — deterministic, zero LLM calls, demo/test safe.
        String headline = applyDeterministicAnalysis(ctx);
        recordAnalysisMeta(ctx, "mock", false, null);
        return AgentAnalysisOutcome.mock(type(), headline);
    }

    @Override
    public String analyze(StartupContext ctx) {
        return runAnalysis(ctx).headlineMessage();
    }

    // ---- Subclass responsibilities ------------------------------------------

    /** Real path: LLM call → validate → normalise → apply to state. Returns headline. Throws on failure. */
    protected abstract String applyRealAnalysis(StartupContext ctx);

    /** Deterministic (Phase 1) analysis and fallback. Returns headline. Must not throw. */
    protected abstract String applyDeterministicAnalysis(StartupContext ctx);

    /** Persist provider / failure metadata onto this department's plan entity in the context. */
    protected abstract void recordAnalysisMeta(StartupContext ctx, String provider, boolean failed, String error);

    // ---- Shared prompt helpers ----------------------------------------------

    /**
     * The CEO's structured analysis, formatted for injection into a department's
     * user prompt. This is the single most important cross-agent contract: every
     * department reasons <em>on top of</em> the CEO's direction, which is
     * verifiable in the composed prompt (and in tests via the fake LLM).
     */
    protected String ceoContextBlock(StartupContext ctx) {
        Startup s = ctx.getStartup();
        StringBuilder sb = new StringBuilder();
        sb.append("=== CEO STRATEGIC ANALYSIS (build directly on this) ===\n");
        appendIf(sb, "Executive summary", s.getExecutiveSummary());
        appendIf(sb, "Problem", s.getProblem());
        appendIf(sb, "Target customer", s.getTargetAudience());
        appendIf(sb, "Value proposition", s.getValueProposition());
        appendIf(sb, "Business model", s.getBusinessModel());
        appendIf(sb, "Recommended MVP direction", s.getMvpDirection());
        appendIf(sb, "Strategic objectives", s.getStrategicObjectives());
        appendIf(sb, "Proposed solution", s.getSolution());
        if (sb.length() == "=== CEO STRATEGIC ANALYSIS (build directly on this) ===\n".length()) {
            sb.append("(No CEO analysis captured yet — infer a sensible direction from the idea.)\n");
        }
        return sb.toString();
    }

    protected static void appendIf(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    /**
     * Phase 3: the agent-to-agent communication context, rendered as two
     * explicitly-labelled sections that are kept <em>separate</em> from the
     * startup/context info, the CEO analysis (section B,
     * {@link #ceoContextBlock}) and any debate transcript (section E):
     * <ul>
     *   <li><b>Section C — messages addressed TO this agent</b> (its inbox), with
     *       the mandated lead line "These are messages specifically addressed to
     *       you." so the model treats them as direct communication, not
     *       background context;</li>
     *   <li><b>Section D — messages this agent has SENT</b>, so it can see its own
     *       side of the conversation and follow up coherently.</li>
     * </ul>
     * Both are populated by the orchestrator from the <em>persisted</em> store
     * before this agent runs; an empty section renders "(none)".
     */
    protected String communicationBlock(StartupContext ctx, AgentType self) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MESSAGES ADDRESSED TO YOU ===\n");
        sb.append("These are messages specifically addressed to you by other departments. "
                + "Read them and factor them into your analysis.\n");
        List<AgentMessage> inbox = ctx.inboxFor(self);
        if (inbox.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (AgentMessage m : inbox) {
                sb.append("- From ").append(m.getAgentType().getDisplayName());
                if (notBlank(m.getSubject())) {
                    sb.append(" — ").append(m.getSubject().trim());
                }
                sb.append(": ").append(m.getContent() == null ? "" : m.getContent().trim()).append('\n');
            }
        }
        sb.append("=== MESSAGES YOU SENT ===\n");
        List<AgentMessage> outbox = ctx.sentFor(self);
        if (outbox.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (AgentMessage m : outbox) {
                sb.append("- To ")
                        .append(m.getTargetAgent() == null ? "(unaddressed)" : m.getTargetAgent().getDisplayName());
                if (notBlank(m.getSubject())) {
                    sb.append(" — ").append(m.getSubject().trim());
                }
                sb.append(": ").append(m.getContent() == null ? "" : m.getContent().trim()).append('\n');
            }
        }
        return sb.toString();
    }

    protected static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Phase 5B — section E: the relevant persistent-memory block for this turn.
     * Returns "" on the plain analysis/tool path (no memory-aware wrapper ran), so
     * every existing Phase 2/3/4 prompt is byte-for-byte unchanged; on the
     * memory-aware path it returns the bounded block the
     * {@link AgentMemoryContextBuilder} rendered and stored on the context. The
     * block is a distinct, labelled section — never merged with the Phase 3
     * communication sections (requirement 13).
     */
    protected String memoryBlock(StartupContext ctx) {
        return ctx.relevantMemoryBlock();
    }

    // ---- Phase 5B: memory-aware reasoning entry points -----------------------

    /**
     * Memory-aware counterpart to {@link #runAnalysis(StartupContext)}. The
     * {@link AgentMemoryContextBuilder} is passed as a <em>method parameter</em>
     * (the Phase 4B precedent) so the {@code new XAgent(llm, props)} constructors —
     * and every Phase 2/3/4 test — stay unchanged. The lifecycle:
     * <ol>
     *   <li>retrieve + rank + bound the relevant persistent memory and set it as
     *       section E on the context (so each subclass {@code buildUserPrompt}
     *       injects it into the single structured call — no extra LLM call);</li>
     *   <li>run the ordinary analysis;</li>
     *   <li>only on a <em>successful</em> real analysis, persist the memory-creation
     *       intents the model expressed (bounded, validated, de-duplicated). A
     *       failed analysis, or the SCRIPTED_DEMO path (which never queues intents),
     *       persists nothing — queued intents are discarded.</li>
     * </ol>
     * Memory creation is thus intentional and REAL-only; it never happens from a
     * failed analysis or a scripted response. Never throws.
     */
    public AgentAnalysisOutcome runAnalysisWithMemory(StartupContext ctx, AgentMemoryContextBuilder memory) {
        ctx.setRelevantMemory(memory.relevantMemoryBlock(ctx.startupId(), type()));
        AgentAnalysisOutcome outcome = runAnalysis(ctx);

        List<AgentMemoryIntent> intents = ctx.drainMemoryIntents().stream()
                .filter(c -> c.author() == type())
                .map(StartupContext.MemoryIntentCandidate::intent)
                .toList();
        if (!outcome.failed()) {
            memory.persistIntents(ctx.startupId(), type(), intents);
        } else {
            log.debug("{} analysis failed — {} queued memory intent(s) discarded (no persistence).",
                    type(), intents.size());
        }
        return outcome;
    }

    /**
     * Memory-aware counterpart to {@link #reasonWithTools(StartupContext, AgentToolReasoner)}.
     * The relevant persistent memory is retrieved and bounded <em>once</em>, up
     * front, and carried through the reasoner's {@code contextBlock} (section E of
     * {@link #toolReasoningContext}) for the whole bounded loop — it is never
     * re-retrieved per tool call, and it stays separate from the tool-interaction
     * history. This path is retrieval-only: tool reasoning does not create memory.
     */
    public ToolAssistedOutcome reasonWithToolsAndMemory(StartupContext ctx, AgentToolReasoner reasoner,
                                                        AgentMemoryContextBuilder memory) {
        ctx.setRelevantMemory(memory.relevantMemoryBlock(ctx.startupId(), type()));
        return reasonWithTools(ctx, reasoner);
    }

    // ---- Phase 4B: genuine, LLM-driven tool use -----------------------------

    /**
     * Run a bounded, LLM-driven tool-assisted reasoning turn for this department.
     * The behaviour is <b>identical</b> for every department: the model is shown
     * the whole tool catalogue (built from the registry by the reasoner) and
     * decides for itself whether and which tool to use — there is deliberately
     * NO {@code agent -> tool} mapping here. Honours the same authority gate as
     * the single-shot path: when {@link #usesRealLlm()} is false no tool
     * selection is attempted and nothing is fabricated. Never throws.
     *
     * <p>This is a separate capability from {@link #runAnalysis(StartupContext)}:
     * it does not alter the single structured call the Phase 2 analysis makes, and
     * tools never mutate the analysis DTOs — the result flows back to the LLM for
     * reasoning (see {@link ToolAssistedOutcome}).
     */
    public ToolAssistedOutcome reasonWithTools(StartupContext ctx, AgentToolReasoner reasoner) {
        String persona = "You are the " + type().getDisplayName() + " agent of an early-stage startup. "
                + "Mandate: " + type().getMandate() + ". Reason concisely and decisively.";
        return reasoner.reason(llm, usesRealLlm(), persona, toolReasoningContext(ctx),
                type().name(), ctx.startupId(), ctx);
    }

    /**
     * Sections A–D for a tool-reasoning prompt, shared by every department:
     * startup/context (A), the CEO's analysis (B, {@link #ceoContextBlock}) and the
     * Phase 3 communication context (C inbox + D sent, {@link #communicationBlock}).
     * It is the same upstream context the single-shot analysis builds on, minus the
     * "produce the analysis JSON" trailer — the reasoner supplies the tool protocol
     * (sections E–H) so no tool definitions are duplicated per agent.
     */
    protected String toolReasoningContext(StartupContext ctx) {
        Startup s = ctx.getStartup();
        StringBuilder sb = new StringBuilder();
        sb.append("=== STARTUP CONTEXT ===\n");
        appendIf(sb, "Startup name", s.getName());
        appendIf(sb, "Founder's idea", IdeaAnalyzer.oneLine(ctx.idea()));
        appendIf(sb, "Your mandate", type().getMandate());
        sb.append('\n');
        sb.append(ceoContextBlock(ctx)).append('\n');
        sb.append(communicationBlock(ctx, type()));
        String memory = memoryBlock(ctx);
        if (!memory.isEmpty()) {
            sb.append('\n').append(memory);
        }
        return sb.toString();
    }

    protected static String joinCsv(List<String> items) {
        return items == null || items.isEmpty() ? null : String.join(", ", items);
    }

    protected static String joinSentences(List<String> items) {
        return items == null || items.isEmpty() ? null : String.join("; ", items);
    }

    protected static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    protected static String shortError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    private String loadPrompt(String path) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Could not load system prompt from {} for {}", path, type(), e);
            return "You are the " + type().getDisplayName() + " agent of an early-stage startup. "
                    + "Mandate: " + type().getMandate() + ". Build on the CEO's analysis and respond with a "
                    + "single JSON object and nothing else.";
        }
    }
}
