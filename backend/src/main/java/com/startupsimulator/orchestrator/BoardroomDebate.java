package com.startupsimulator.orchestrator;

import com.startupsimulator.agent.AgentAnalysisException;
import com.startupsimulator.agent.CeoAgent;
import com.startupsimulator.agent.CeoAnalysisException;
import com.startupsimulator.agent.DebateResponse;
import com.startupsimulator.agent.DecisionSynthesisResponse;
import com.startupsimulator.agent.DeveloperAgent;
import com.startupsimulator.agent.FinanceAgent;
import com.startupsimulator.agent.IdeaAnalyzer;
import com.startupsimulator.agent.LLMService;
import com.startupsimulator.agent.MarketingAgent;
import com.startupsimulator.agent.StartupContext;
import com.startupsimulator.config.LlmProperties;
import com.startupsimulator.model.AgentMessage;
import com.startupsimulator.model.Budget;
import com.startupsimulator.model.Debate;
import com.startupsimulator.model.Decision;
import com.startupsimulator.model.MarketingPlan;
import com.startupsimulator.model.MvpFeature;
import com.startupsimulator.model.Startup;
import com.startupsimulator.model.TechnicalPlan;
import com.startupsimulator.model.enums.AgentState;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.DebateStatus;
import com.startupsimulator.model.enums.EventType;
import com.startupsimulator.model.enums.SimulationPhase;
import com.startupsimulator.service.AgentInbox;
import com.startupsimulator.service.AgentService;
import com.startupsimulator.service.DebateService;
import com.startupsimulator.service.DecisionService;
import com.startupsimulator.service.EventService;
import com.startupsimulator.service.MessageService;
import com.startupsimulator.service.StartupContextService;
import com.startupsimulator.service.StartupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Phase 2C — the autonomous boardroom debate.
 *
 * <p>Replaces the old scripted debate/decision steps. Given the four real Phase 2B
 * analyses already on the {@link StartupContext}, it runs a bounded 3-round debate
 * (POSITION → CHALLENGE → CONVERGENCE) between Development, Marketing and Finance,
 * then has the CEO synthesise a final decision. Every debate turn is a real
 * {@link LLMService#generateStructured} call returning a typed {@link DebateResponse};
 * the CEO synthesis returns a typed {@link DecisionSynthesisResponse}. Ten LLM calls
 * total (3+3+3 turns + 1 synthesis); the CEO's Round-1 framing is deterministic.
 *
 * <p>Every Round-2/Round-3/synthesis prompt embeds the full running transcript, so
 * each participant genuinely reads what the others said (§21). A failed turn falls
 * back to a deterministic response and the debate finishes
 * {@link DebateStatus#COMPLETED_WITH_WARNINGS} rather than crashing (§17).
 */
@Slf4j
@Component
public class BoardroomDebate {

    private static final List<AgentType> DEPARTMENTS =
            List.of(AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE);
    private static final List<AgentType> SEATING =
            List.of(AgentType.CEO, AgentType.DEVELOPMENT, AgentType.MARKETING, AgentType.FINANCE);

    private final MessageService messageService;
    private final DebateService debateService;
    private final DecisionService decisionService;
    private final EventService eventService;
    private final AgentService agentService;
    private final StartupContextService contextService;
    private final StartupService startupService;
    @SuppressWarnings("unused")
    private final CeoAgent ceoAgent;
    private final DeveloperAgent developerAgent;
    private final MarketingAgent marketingAgent;
    private final FinanceAgent financeAgent;
    private final LLMService llm;
    private final LlmProperties llmProperties;

    @Value("${app.simulation.tick-interval-ms:900}")
    private long tickIntervalMs;

    public BoardroomDebate(MessageService messageService, DebateService debateService,
                           DecisionService decisionService, EventService eventService,
                           AgentService agentService, StartupContextService contextService,
                           StartupService startupService, CeoAgent ceoAgent,
                           DeveloperAgent developerAgent, MarketingAgent marketingAgent,
                           FinanceAgent financeAgent, LLMService llm, LlmProperties llmProperties) {
        this.messageService = messageService;
        this.debateService = debateService;
        this.decisionService = decisionService;
        this.eventService = eventService;
        this.agentService = agentService;
        this.contextService = contextService;
        this.startupService = startupService;
        this.ceoAgent = ceoAgent;
        this.developerAgent = developerAgent;
        this.marketingAgent = marketingAgent;
        this.financeAgent = financeAgent;
        this.llm = llm;
        this.llmProperties = llmProperties;
    }

    /**
     * The authoritative gate for genuine LLM reasoning in the debate: REAL mode
     * requested <em>and</em> a real provider available. When this is false the debate
     * runs its deterministic script with zero LLM calls (scripted demo, or REAL
     * requested with no real provider — the latter degrades to the script rather than
     * crashing a running simulation).
     */
    private boolean useRealLlm() {
        return llmProperties.isRealMode() && llm.isRealProvider();
    }

    /** One in-memory debate turn, used to build the running transcript for later prompts. */
    private record Turn(AgentType speaker, int round, String type, AgentType target, String content) {}

    /**
     * Run the full debate + CEO synthesis + final decision. Assumes the ANALYSIS
     * phase has completed and left the four analyses on {@code ctx}. Leaves the
     * startup in the {@link SimulationPhase#PLAN} phase for the orchestrator to
     * finalize.
     */
    public void conduct(StartupContext ctx, double speed) throws InterruptedException {
        Long id = ctx.startupId();

        Debate debate = debateService.create(id,
                "MVP scope, positioning & launch timeline",
                "Given each department's analysis, what should the V1 MVP include, what should "
                        + "wait, and can we launch within ~3 months without hurting positioning or runway?");
        Long debateId = debate.getId();

        eventService.record(id, EventType.DEBATE_STARTED,
                "Boardroom debate opened: " + debate.getTopic() + ".",
                Map.of("debateId", debateId, "topic", debate.getTopic()));

        for (AgentType type : SEATING) {
            agentService.setState(id, type, AgentState.DISCUSSING, "In the boardroom.");
            eventService.record(id, EventType.AGENT_JOINED_DEBATE,
                    type.getDisplayName() + " entered the boardroom.",
                    debatePayload(type, 0, "SEATED", null, debateId));
        }
        sleep(speed);

        List<Turn> transcript = new ArrayList<>();
        boolean hadFailure = false;

        // ---- Round 1: POSITION -------------------------------------------------
        startRound(id, 1, "POSITION", "Each department presents its recommendation.");
        postFraming(ctx, debateId, transcript, speed);
        for (AgentType type : DEPARTMENTS) {
            hadFailure |= runTurn(ctx, debateId, type, 1, "POSITION", null, transcript, speed);
        }
        completeRound(id, 1, "POSITION");

        // ---- Round 2: CHALLENGE ------------------------------------------------
        startRound(id, 2, "CHALLENGE", "Each department challenges another's key assumption.");
        hadFailure |= runTurn(ctx, debateId, AgentType.DEVELOPMENT, 2, "CHALLENGE", AgentType.MARKETING, transcript, speed);
        hadFailure |= runTurn(ctx, debateId, AgentType.MARKETING, 2, "CHALLENGE", AgentType.DEVELOPMENT, transcript, speed);
        hadFailure |= runTurn(ctx, debateId, AgentType.FINANCE, 2, "CHALLENGE", AgentType.DEVELOPMENT, transcript, speed);
        completeRound(id, 2, "CHALLENGE");

        // ---- Round 3: CONVERGENCE ---------------------------------------------
        startRound(id, 3, "CONVERGENCE",
                "Each department gives a revised recommendation, concessions and remaining concern.");
        for (AgentType type : DEPARTMENTS) {
            hadFailure |= runTurn(ctx, debateId, type, 3, "CONVERGENCE", null, transcript, speed);
        }
        completeRound(id, 3, "CONVERGENCE");

        eventService.record(id, EventType.DEBATE_COMPLETED,
                "Boardroom debate concluded after 3 rounds.",
                Map.of("debateId", debateId, "rounds", 3, "hadFailure", hadFailure));

        runSynthesisAndDecision(ctx, debateId, transcript, hadFailure, speed);
    }

    private void startRound(Long id, int round, String type, String detail) {
        eventService.record(id, EventType.DEBATE_ROUND_STARTED,
                "Round " + round + " (" + type + ") — " + detail,
                Map.of("round", round, "roundType", type));
    }

    private void completeRound(Long id, int round, String type) {
        eventService.record(id, EventType.DEBATE_ROUND_COMPLETED,
                "Round " + round + " (" + type + ") complete.",
                Map.of("round", round, "roundType", type));
    }

    private void postFraming(StartupContext ctx, Long debateId, List<Turn> transcript, double speed)
            throws InterruptedException {
        Long id = ctx.startupId();
        agentService.setState(id, AgentType.CEO, AgentState.DISCUSSING, "Framing the debate.");
        String content = ceoFramingMessage(ctx);
        messageService.create(id, AgentType.CEO, content, debateId, 1, "FRAMING", null);
        transcript.add(new Turn(AgentType.CEO, 1, "FRAMING", null, content));
        eventService.record(id, EventType.AGENT_DEBATE_MESSAGE, content,
                debatePayload(AgentType.CEO, 1, "FRAMING", null, debateId));
        sleep(speed);
    }

    /**
     * Run one department's debate turn. Returns {@code true} if the turn's LLM call
     * failed and a deterministic fallback was used (§17). When the provider is a mock,
     * the deterministic response is used directly with no call.
     */
    private boolean runTurn(StartupContext ctx, Long debateId, AgentType type, int round,
                            String turnType, AgentType target, List<Turn> transcript, double speed)
            throws InterruptedException {
        Long id = ctx.startupId();
        agentService.setState(id, type, AgentState.DISCUSSING, turnActivity(turnType, target));
        eventService.record(id, EventType.AGENT_DEBATE_STARTED,
                type.getDisplayName() + " is " + turnGerund(turnType) + "...",
                debatePayload(type, round, turnType, target, debateId));

        DebateResponse response;
        boolean failed = false;
        if (useRealLlm()) {
            String system = debateSystemPrompt(type);
            String user = buildTurnPrompt(ctx, type, turnType, target, transcript);
            try {
                DebateResponse raw = llm.generateStructured(system, user, DebateResponse.class);
                if (raw == null || !raw.isValid()) {
                    throw new CeoAnalysisException(type + " debate turn returned no usable content.");
                }
                response = raw.normalized();
            } catch (CeoAnalysisException | AgentAnalysisException e) {
                log.warn("{} round {} ({}) debate turn failed ({}); using deterministic fallback.",
                        type, round, turnType, e.getMessage());
                response = fallbackResponse(ctx, type, round).normalized();
                failed = true;
            }
        } else {
            response = fallbackResponse(ctx, type, round).normalized();
        }

        // Phase 3 (req 8): in a CHALLENGE turn the LLM may name whom it challenges via
        // DebateResponse.challengeTarget. Validate it against AgentType and use it as the
        // addressable target; on an unresolvable/self target keep the seating default and
        // warn — never silently redirect the challenge to a different agent.
        AgentType effectiveTarget = resolveChallengeTarget(ctx, type, round, turnType, target, response, debateId);

        String content = renderMessage(response);
        messageService.create(id, type, content, debateId, round, turnType, effectiveTarget);
        transcript.add(new Turn(type, round, turnType, effectiveTarget, content));
        eventService.record(id, EventType.AGENT_DEBATE_MESSAGE, content,
                debatePayload(type, round, turnType, effectiveTarget, debateId));

        Map<String, Object> done = debatePayload(type, round, turnType, effectiveTarget, debateId);
        done.put("fallback", failed);
        eventService.record(id, EventType.AGENT_DEBATE_COMPLETED,
                type.getDisplayName() + (failed ? " finished its turn (deterministic fallback)." : " finished its turn."),
                done);
        sleep(speed);
        return failed;
    }

    /**
     * Build a department's turn prompt. Always embeds the idea, all four Phase 2B
     * analyses and the full running transcript, so Round-2/Round-3 turns provably see
     * the earlier rounds' messages (§21).
     */
    private String buildTurnPrompt(StartupContext ctx, AgentType type, String turnType,
                                   AgentType target, List<Turn> transcript) {
        StringBuilder sb = new StringBuilder();
        sb.append("Founder's idea: ").append(IdeaAnalyzer.oneLine(ctx.idea())).append("\n\n");
        sb.append(analysesBlock(ctx)).append('\n');
        sb.append(transcriptBlock(transcript)).append('\n');
        sb.append(addressedMessagesBlock(ctx, type)).append('\n');
        sb.append("=== YOUR TASK ===\n");
        sb.append("You are the ").append(type.getDisplayName()).append(" department. ");
        switch (turnType) {
            case "POSITION" -> sb.append("Round 1 — present your position and a concrete recommendation for the "
                    + "V1 MVP scope and launch timeline, grounded in your own analysis above.");
            case "CHALLENGE" -> sb.append("Round 2 — you have read the other departments' Round 1 positions above. "
                    + "Directly challenge a key assumption or claim the ")
                    .append(target == null ? "other" : target.getDisplayName())
                    .append(" department made in Round 1. Paraphrase what they said and explain why you disagree; "
                            + "put each challenge in the 'challenges' array.");
            case "CONVERGENCE" -> sb.append("Round 3 — having heard the challenges above, give your revised "
                    + "recommendation. List what you now concede in 'concessions' and your single remaining concern "
                    + "in 'concerns'.");
            default -> sb.append("Present your view.");
        }
        sb.append("\n\nRespond with a single JSON object with fields: position, reasoning, challenges (array), "
                + "concessions (array), recommendation, confidence (0-100), concerns (array)");
        if ("CHALLENGE".equals(turnType)) {
            sb.append(", challengeTarget (the exact department name — Development, Marketing or Finance — "
                    + "whose assumption you are challenging; this addresses your challenge to that department)");
        }
        sb.append(". Return only JSON.");
        return sb.toString();
    }

    /**
     * Phase 3 (req 7/8): the messages addressed TO this department (section C) and the
     * messages it has SENT (section D), read from the PERSISTED store via
     * {@link MessageService#addressedTo}/{@link MessageService#sentBy} — not the
     * in-memory transcript. This is the step that lets a department's later reasoning
     * genuinely see a challenge an earlier turn addressed to it through the real
     * persistence path. Kept distinct from the department analyses (section B) and the
     * debate transcript (section E) so the sections are never collapsed into one block.
     */
    private String addressedMessagesBlock(StartupContext ctx, AgentType self) {
        Long id = ctx.startupId();
        StringBuilder sb = new StringBuilder();
        sb.append("=== MESSAGES ADDRESSED TO YOU ===\n");
        sb.append("These are messages specifically addressed to you by other departments in this debate. "
                + "Read them and respond to them directly.\n");
        List<AgentMessage> inbox = messageService.addressedTo(id, self);
        if (inbox == null || inbox.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (AgentMessage m : inbox) {
                sb.append("- From ").append(m.getAgentType().getDisplayName());
                if (m.getDebateRound() != null) {
                    sb.append(" [Round ").append(m.getDebateRound()).append(']');
                }
                sb.append(": ").append(m.getContent() == null ? "" : m.getContent().trim()).append('\n');
            }
        }
        sb.append("=== MESSAGES YOU SENT ===\n");
        List<AgentMessage> outbox = messageService.sentBy(id, self);
        if (outbox == null || outbox.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (AgentMessage m : outbox) {
                sb.append("- To ")
                        .append(m.getTargetAgent() == null ? "the room" : m.getTargetAgent().getDisplayName());
                sb.append(": ").append(m.getContent() == null ? "" : m.getContent().trim()).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Phase 3 (req 8): resolve the addressable target for a turn. For a CHALLENGE the
     * LLM may name a department in {@link DebateResponse#challengeTarget()}; it is
     * validated against {@link AgentType} via {@link AgentInbox#resolveRecipient}. A
     * valid, non-self department becomes the target; a blank value keeps the seating
     * default silently; an unresolvable or self value keeps the default AND emits an
     * {@code AGENT_MESSAGE_FAILED} warning (no silent redirect). Non-CHALLENGE turns
     * always keep the given target.
     */
    private AgentType resolveChallengeTarget(StartupContext ctx, AgentType speaker, int round,
                                             String turnType, AgentType seatingTarget,
                                             DebateResponse response, Long debateId) {
        if (!"CHALLENGE".equals(turnType)) {
            return seatingTarget;
        }
        String named = response.challengeTarget();
        if (named == null || named.isBlank()) {
            return seatingTarget;              // null = keep the default seating-based target
        }
        Optional<AgentType> chosen = AgentInbox.resolveRecipient(named);
        if (chosen.isPresent() && chosen.get() != speaker) {
            return chosen.get();
        }
        eventService.record(ctx.startupId(), EventType.AGENT_MESSAGE_FAILED,
                speaker.getDisplayName() + " named an invalid challenge target '" + named.trim()
                        + "'; keeping the default target ("
                        + (seatingTarget == null ? "the room" : seatingTarget.getDisplayName()) + ").",
                debatePayload(speaker, round, turnType, seatingTarget, debateId));
        return seatingTarget;
    }

    /**
     * CEO synthesises the whole debate into a final decision, applies the outcome to
     * the MVP (deferring named non-scanner features), recomputes finances and persists
     * the structured decision. Moves the startup from DECISION into the PLAN phase.
     */
    private void runSynthesisAndDecision(StartupContext ctx, Long debateId, List<Turn> transcript,
                                         boolean hadFailure, double speed) throws InterruptedException {
        Long id = ctx.startupId();
        Startup startup = ctx.getStartup();

        startup.setCurrentPhase(SimulationPhase.DECISION);
        startupService.save(startup);

        agentService.setState(id, AgentType.CEO, AgentState.DECIDING, "Synthesising the debate.");
        eventService.record(id, EventType.CEO_SYNTHESIS_STARTED,
                "The CEO is weighing the debate and making the final call...",
                agentPayload(AgentType.CEO, AgentState.DECIDING, debateId));
        eventService.record(id, EventType.DECISION_STARTED,
                "Final decision in progress.", Map.of("debateId", debateId));
        sleep(speed);

        DecisionSynthesisResponse synthesis;
        boolean synthesisFailed = false;
        boolean real = useRealLlm();
        if (real) {
            try {
                DecisionSynthesisResponse raw = llm.generateStructured(
                        synthesisSystemPrompt(), buildSynthesisPrompt(ctx, transcript),
                        DecisionSynthesisResponse.class);
                if (raw == null || !raw.isValid()) {
                    throw new CeoAnalysisException("CEO synthesis returned no usable content.");
                }
                synthesis = raw.normalized();
            } catch (CeoAnalysisException | AgentAnalysisException e) {
                log.warn("CEO synthesis failed ({}); using deterministic synthesis.", e.getMessage());
                synthesis = deterministicSynthesis(ctx).normalized();
                synthesisFailed = true;
                real = false; // fell back to the script — treat the outcome as scripted below
            }
        } else {
            synthesis = deterministicSynthesis(ctx).normalized();
        }

        // Apply the debate outcome to the MVP. In REAL mode the CEO's deferral list is
        // authoritative (honoured exactly, including the scanner if the model named it,
        // and no forced heaviest-feature cut). In scripted/fallback mode the legacy
        // protections apply so the Phase 1/2C blueprint stays stable.
        applyDeferrals(ctx, synthesis.deferredFeatures(), real);
        // Finance: in REAL mode recompute runway from the authoritative LLM budget
        // without re-clamping cost/burn/health; in scripted mode apply the fixed
        // scope-reduction figures.
        String financeMessage = real
                ? financeAgent.recomputeRunwayMessage(ctx)
                : financeAgent.applyScopeReduction(ctx);
        contextService.persistScopeChange(ctx);
        startup.setConstraints(notBlank(synthesis.finalMvpDirection())
                ? synthesis.finalMvpDirection()
                : "V1 focuses on the core experience; heavier non-core features move to V2.");

        // Persist the structured synthesis as the authoritative final decision.
        Decision decision = decisionService.createFromSynthesis(id, debateId, synthesis, DEPARTMENTS);

        // Finance's recomputation, recorded as a Round-3 debate message + budget event.
        messageService.create(id, AgentType.FINANCE, financeMessage, debateId, 3, "CONVERGENCE", null);
        eventService.record(id, EventType.BUDGET_UPDATED, financeMessage,
                Map.of("budgetRemaining", startup.getBudgetRemaining(),
                        "runwayMonths", startup.getRunwayMonths()));

        // The CEO's synthesis, posted as a SYNTHESIS message so the boardroom shows it.
        String synthesisMessage = renderSynthesis(synthesis);
        messageService.create(id, AgentType.CEO, synthesisMessage, debateId, 3, "SYNTHESIS", null);
        eventService.record(id, EventType.AGENT_DEBATE_MESSAGE, synthesisMessage,
                debatePayload(AgentType.CEO, 3, "SYNTHESIS", null, debateId));

        eventService.record(id, EventType.DECISION_PROPOSED, "Decision: " + synthesis.decision(),
                Map.of("decisionId", decision.getId(), "debateId", debateId));
        eventService.record(id, EventType.CEO_SYNTHESIS_COMPLETED, "CEO synthesis complete.",
                agentPayload(AgentType.CEO, AgentState.DECIDING, decision.getId()));
        eventService.record(id, EventType.DECISION_APPROVED, synthesis.decision(),
                Map.of("decisionId", decision.getId()));

        boolean warnings = hadFailure || synthesisFailed;
        debateService.complete(debateId, warnings ? DebateStatus.COMPLETED_WITH_WARNINGS : DebateStatus.RESOLVED);

        updateHealthAfterDecision(startup);
        startup.setCurrentPhase(SimulationPhase.PLAN);
        startupService.save(startup);

        eventService.record(id, EventType.MVP_UPDATED, "MVP scope finalised from the debate.",
                Map.of("mvpProgress", startup.getMvpProgress()));
        eventService.record(id, EventType.DECISION_COMPLETED, "Final decision recorded.",
                Map.of("decisionId", decision.getId(), "debateId", debateId,
                        "status", warnings ? DebateStatus.COMPLETED_WITH_WARNINGS.name() : DebateStatus.RESOLVED.name()));
        eventService.record(id, EventType.PHASE_COMPLETED, "Decision phase complete.",
                Map.of("phase", SimulationPhase.DECISION.name()));
        sleep(speed);
    }

    private String buildSynthesisPrompt(StartupContext ctx, List<Turn> transcript) {
        StringBuilder sb = new StringBuilder();
        sb.append("Founder's idea: ").append(IdeaAnalyzer.oneLine(ctx.idea())).append("\n\n");
        sb.append(analysesBlock(ctx)).append('\n');
        sb.append(transcriptBlock(transcript)).append('\n');
        sb.append("=== YOUR TASK (CEO) ===\n");
        sb.append("You have every department analysis and the full 3-round debate above. Make the final call. "
                + "Weigh the unresolved disagreements. Decide what V1 should include and what to defer. List the "
                + "exact feature names to move to V2 in 'deferredFeatures' (match the proposed MVP feature names). "
                + "Do NOT defer the product scanner — Marketing established it is core to differentiation.\n\n");
        sb.append("Respond with a single JSON object: decision, rationale, acceptedArguments (array), "
                + "rejectedArguments (array), finalMvpDirection, deferredFeatures (array), finalRisks (array), "
                + "finalPriorities (array). Return only JSON.");
        return sb.toString();
    }

    // ---- Prompt building blocks ---------------------------------------------

    private String ceoFramingMessage(StartupContext ctx) {
        Startup s = ctx.getStartup();
        String steer = notBlank(s.getMvpDirection())
                ? lowerFirst(s.getMvpDirection().trim())
                : "ship a focused MVP fast while protecting our runway";
        return "We've each analysed \"" + IdeaAnalyzer.oneLine(ctx.idea()) + "\". My steer is to "
                + steer + (steer.endsWith(".") ? " " : ". ")
                + "Development, Marketing, Finance — give me your positions on scope and timeline, "
                + "then challenge each other. I'll make the final call.";
    }

    /** All four Phase 2B analyses, summarised as the shared factual basis for the debate. */
    private String analysesBlock(StartupContext ctx) {
        Startup s = ctx.getStartup();
        TechnicalPlan tp = ctx.getTechnicalPlan();
        MarketingPlan mp = ctx.getMarketingPlan();
        Budget b = ctx.getBudget();
        StringBuilder sb = new StringBuilder("=== DEPARTMENT ANALYSES (the real inputs to this debate) ===\n");

        sb.append("[CEO] ");
        appendInline(sb, "Executive summary", s.getExecutiveSummary());
        appendInline(sb, "Recommended MVP direction", s.getMvpDirection());
        appendInline(sb, "Problem", s.getProblem());
        sb.append('\n');

        sb.append("[Development] ");
        appendInline(sb, "Solution", s.getSolution());
        if (tp != null) {
            appendInline(sb, "Architecture", tp.getArchitecture());
            appendInline(sb, "Timeline", tp.getTimeline());
            if (tp.getEstimatedEngineeringMonths() > 0) {
                appendInline(sb, "Est. engineering months",
                        trimNumber(tp.getEstimatedEngineeringMonths()));
            }
        }
        sb.append('\n');

        sb.append("[Marketing] ");
        if (mp != null) {
            appendInline(sb, "Positioning", mp.getPositioning());
            appendInline(sb, "Pricing", mp.getPricingStrategy());
            appendInline(sb, "Go-to-market", mp.getGoToMarket());
        }
        sb.append('\n');

        sb.append("[Finance] ");
        if (b != null) {
            if (b.getMonthlyBurn() > 0) appendInline(sb, "Monthly burn", fmt(b.getMonthlyBurn()));
            if (b.getRunwayMonths() > 0) appendInline(sb, "Runway (months)", trimNumber(b.getRunwayMonths()));
            appendInline(sb, "Break-even", b.getBreakEvenAssumption());
        }
        sb.append('\n');

        sb.append("Proposed MVP features: ").append(featureList(ctx)).append('\n');
        return sb.toString();
    }

    private String featureList(StartupContext ctx) {
        List<MvpFeature> features = ctx.getMvpFeatures();
        if (features == null || features.isEmpty()) {
            return "(none drafted yet)";
        }
        StringBuilder sb = new StringBuilder();
        for (MvpFeature f : features) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(f.getName()).append(f.isInMvp() ? " [V1]" : " [V2]");
        }
        return sb.toString();
    }

    private String transcriptBlock(List<Turn> transcript) {
        if (transcript.isEmpty()) {
            return "=== DEBATE TRANSCRIPT SO FAR ===\n(No turns yet — you are opening the debate.)\n";
        }
        StringBuilder sb = new StringBuilder("=== DEBATE TRANSCRIPT SO FAR ===\n");
        for (Turn t : transcript) {
            sb.append("[Round ").append(t.round()).append(" · ").append(t.type());
            if (t.target() != null) sb.append(" → ").append(t.target().getDisplayName());
            sb.append("] ").append(t.speaker().getDisplayName()).append(": ")
                    .append(t.content()).append('\n');
        }
        return sb.toString();
    }

    // ---- Rendering structured responses into readable messages --------------

    private String renderMessage(DebateResponse r) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(r.position())) sb.append(r.position().trim());
        if (notBlank(r.reasoning())) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(r.reasoning().trim());
        }
        if (r.challenges() != null && !r.challenges().isEmpty()) {
            sb.append("\nChallenges: ").append(String.join("; ", r.challenges()));
        }
        if (r.concessions() != null && !r.concessions().isEmpty()) {
            sb.append("\nConcedes: ").append(String.join("; ", r.concessions()));
        }
        if (notBlank(r.recommendation())) {
            sb.append("\nRecommendation: ").append(r.recommendation().trim());
        }
        if (r.concerns() != null && !r.concerns().isEmpty()) {
            sb.append("\nRemaining concern: ").append(String.join("; ", r.concerns()));
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "(no comment)" : out;
    }

    private String renderSynthesis(DecisionSynthesisResponse s) {
        StringBuilder sb = new StringBuilder(s.decision().trim());
        if (notBlank(s.rationale())) sb.append(' ').append(s.rationale().trim());
        return sb.toString();
    }

    // ---- Deterministic fallbacks (§17) --------------------------------------

    /**
     * Deterministic per-role, per-round debate response, used when the LLM turn
     * fails or when the provider is a mock. Round-1 reuses each agent's existing
     * scripted stance; later rounds preserve genuine cross-department disagreement.
     */
    private DebateResponse fallbackResponse(StartupContext ctx, AgentType type, int round) {
        return switch (type) {
            case DEVELOPMENT -> switch (round) {
                case 1 -> new DebateResponse(developerAgent.debateStatement(ctx),
                        "Full scope trends toward ~5 months; the core engine and product scanner are the essential parts.",
                        List.of(), List.of(),
                        "Keep the core engine and product scanner in V1; defer the Community feed to V2.",
                        72, List.of());
                case 2 -> new DebateResponse(
                        "From an engineering view the Community feed is the clearest cut.",
                        "It adds real-time infrastructure and moderation cost without being core to the first release.",
                        List.of("Marketing wants maximum surface area, but every extra feature costs weeks — "
                                + "the Community feed is the least defensible for V1."),
                        List.of(),
                        "Cut the Community feed to hit a ~3 month timeline.", 74, List.of());
                default -> new DebateResponse(
                        "Revised: build the core engine, onboarding and product scanner for V1.",
                        "That is the leanest scope that still delivers the core value.",
                        List.of(),
                        List.of("Agree the product scanner stays in V1 — it is core to differentiation."),
                        "Ship the trimmed V1 in ~3 months; Community feed moves to V2.", 78,
                        List.of("Scanner accuracy still needs real-world testing."));
            };
            case MARKETING -> switch (round) {
                case 1 -> new DebateResponse(marketingAgent.debateStatement(ctx),
                        "The product scanner is how users experience the core value and how they tell others about it.",
                        List.of(), List.of(),
                        "Keep the product scanner central in V1; it drives differentiation and word of mouth.",
                        70, List.of());
                case 2 -> new DebateResponse(
                        "Trimming too hard risks a launch nobody notices.",
                        "Positioning depends on a visibly differentiated first impression.",
                        List.of("Development frames the scanner as optional scope, but it is precisely "
                                + "what makes us stand out — it cannot be the thing we cut."),
                        List.of(),
                        "Protect the scanner even if other features slip.", 68, List.of());
                default -> new DebateResponse(
                        "Revised: keep the product scanner and a tight onboarding story for V1.",
                        "A focused, differentiated launch beats a broad but unremarkable one.",
                        List.of(),
                        List.of("Accept deferring the Community feed to V2 to protect the timeline."),
                        "Launch V1 around the scanner; add community once we have users.", 74,
                        List.of("Need a referral loop at launch to compensate for the missing community feature."));
            };
            case FINANCE -> switch (round) {
                case 1 -> new DebateResponse(financeAgent.debateStatement(ctx),
                        "A ~5 month build raises upfront cost and shortens runway below a comfortable threshold.",
                        List.of(), List.of(),
                        "Reduce scope to protect the runway; target a ~3 month build.", 76, List.of());
                case 2 -> new DebateResponse(
                        "The timeline Development described is the single biggest cost driver.",
                        "Every extra month of build is roughly another month of burn before any revenue.",
                        List.of("Development's 5-month plan assumes we can afford the burn — the runway math "
                                + "says we cannot carry the Community feed in V1."),
                        List.of(),
                        "Cut scope enough to bring the build to ~3 months.", 77, List.of());
                default -> new DebateResponse(
                        "Revised: a ~3 month build keeps upfront cost and runway healthy.",
                        "Deferring the Community feed materially lowers cost and extends runway.",
                        List.of(),
                        List.of("Agree the scanner stays — it is core and not the expensive part."),
                        "Fund the trimmed V1; revisit the Community feed after launch.", 80,
                        List.of("Freemium conversion must clear ~3-5% to stay on plan."));
            };
            default -> new DebateResponse("Noted.", null, List.of(), List.of(), null, 60, List.of());
        };
    }

    private DecisionSynthesisResponse deterministicSynthesis(StartupContext ctx) {
        return new DecisionSynthesisResponse(
                "Ship a focused V1: keep the core engine, onboarding and product scanner; defer the "
                        + "Community feed to V2; target a launch in about three months.",
                "Development showed the full scope runs ~5 months and Finance flagged runway pressure, while "
                        + "Marketing established that the product scanner is core to differentiation. Cutting the "
                        + "Community feed protects the timeline and runway without sacrificing positioning.",
                List.of("Development: the Community feed is the clearest non-core cut.",
                        "Marketing: the product scanner must stay in V1.",
                        "Finance: a shorter build materially extends runway."),
                List.of("Shipping the full scope, including the Community feed, in the initial timeline."),
                "Core personalised engine + onboarding + product scanner for V1; Community feed in V2.",
                List.of("Community feed"),
                List.of("Adoption risk if the core value is not immediately obvious",
                        "Scanner accuracy needs real-world testing",
                        "Freemium conversion must clear ~3-5%"),
                List.of("Ship the core engine and product scanner", "Nail onboarding and time-to-value",
                        "Protect the runway", "Turn on a referral loop at launch"));
    }

    // ---- Applying the debate outcome to the MVP -----------------------------

    /**
     * Move the CEO's chosen features to V2. Matches by name (case-insensitive, either
     * direction of containment).
     *
     * <p>When {@code authoritative} (REAL mode), the LLM's deferral list is honoured
     * exactly: the product scanner may be deferred if the model named it, and nothing
     * is force-deferred when the list is empty — the model's scope decision stands.
     *
     * <p>Otherwise (scripted/fallback) the legacy Phase 2C protections apply: the
     * product scanner is never deferred, and when no name matches the heaviest
     * non-scanner V1 feature is deterministically deferred so the blueprint still
     * reflects a real scope cut (§11).
     */
    private void applyDeferrals(StartupContext ctx, List<String> deferred, boolean authoritative) {
        List<MvpFeature> features = ctx.getMvpFeatures();
        if (features == null || features.isEmpty()) {
            return;
        }
        List<MvpFeature> toDefer = new ArrayList<>();
        if (deferred != null && !deferred.isEmpty()) {
            for (String name : deferred) {
                if (name == null || name.isBlank()) continue;
                String needle = name.trim().toLowerCase();
                for (MvpFeature f : features) {
                    if (!f.isInMvp()) continue;
                    if (!authoritative && isScanner(f)) continue; // legacy scanner protection
                    String fn = f.getName() == null ? "" : f.getName().toLowerCase();
                    if (fn.equals(needle) || fn.contains(needle) || needle.contains(fn)) {
                        toDefer.add(f);
                    }
                }
            }
        }
        if (toDefer.isEmpty() && !authoritative) {
            // Scripted mode only: guarantee a visible scope cut.
            features.stream()
                    .filter(MvpFeature::isInMvp)
                    .filter(f -> !isScanner(f))
                    .max(Comparator.comparingInt(MvpFeature::getEffort))
                    .ifPresent(toDefer::add);
        }
        for (MvpFeature f : toDefer) {
            f.setInMvp(false);
            f.setTargetRelease("V2");
        }
    }

    private boolean isScanner(MvpFeature f) {
        return f.getName() != null && f.getName().toLowerCase().contains("scanner");
    }

    // ---- System prompts (personalities, §7) ---------------------------------

    private String debateSystemPrompt(AgentType type) {
        String persona = switch (type) {
            case DEVELOPMENT -> "You are the Development lead: technical, focused on feasibility, scope and "
                    + "timeline. You push back on scope creep.";
            case MARKETING -> "You are the Marketing lead: focused on the customer, growth and positioning. "
                    + "You defend what differentiates the product.";
            case FINANCE -> "You are the Finance lead: cost-conscious and risk-aware, focused on burn and runway.";
            case CEO -> "You are the CEO: strategic, decisive, balancing every department.";
        };
        return persona + " You are in a boardroom debate. Argue your genuine position — do not simply agree "
                + "with the others. Be concise and specific. Respond with a single JSON object and nothing else.";
    }

    private String synthesisSystemPrompt() {
        return "You are the CEO of an early-stage startup making the final decision after a boardroom debate. "
                + "You are strategic and decisive: weigh the departments' arguments, resolve the disagreements, "
                + "and commit to a clear, buildable V1. Respond with a single JSON object and nothing else.";
    }

    // ---- Events, health and small utilities ---------------------------------

    private String turnActivity(String turnType, AgentType target) {
        return switch (turnType) {
            case "POSITION" -> "Presenting its position.";
            case "CHALLENGE" -> "Challenging " + (target == null ? "the room" : target.getDisplayName()) + ".";
            case "CONVERGENCE" -> "Giving its revised recommendation.";
            default -> "Debating.";
        };
    }

    private String turnGerund(String turnType) {
        return switch (turnType) {
            case "POSITION" -> "presenting its position";
            case "CHALLENGE" -> "raising a challenge";
            case "CONVERGENCE" -> "converging on a recommendation";
            default -> "debating";
        };
    }

    private Map<String, Object> agentPayload(AgentType type, AgentState state, Object extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agentType", type.name());
        payload.put("agentState", state == null ? null : state.name());
        if (extra != null) {
            payload.put("detail", extra);
        }
        return payload;
    }

    private Map<String, Object> debatePayload(AgentType type, int round, String messageType,
                                              AgentType target, Long debateId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agentType", type.name());
        payload.put("agentState", AgentState.DISCUSSING.name());
        payload.put("debateId", debateId);
        payload.put("round", round);
        payload.put("messageType", messageType);
        if (target != null) {
            payload.put("targetAgent", target.name());
        }
        return payload;
    }

    private void updateHealthAfterDecision(Startup startup) {
        startup.setMvpProgress(75);
        // Trimming scope de-risks delivery and stretches runway.
        startup.setTechnicalFeasibility(Math.min(100, startup.getTechnicalFeasibility() + 8));
        startup.setMarketReadiness(Math.min(100, startup.getMarketReadiness() + 10));
        startup.setOverallProgress(computeOverall(startup));
    }

    private int computeOverall(Startup s) {
        int blended = (int) Math.round(
                0.30 * s.getMvpProgress()
                        + 0.25 * s.getTechnicalFeasibility()
                        + 0.20 * s.getMarketReadiness()
                        + 0.25 * s.getFinancialHealth());
        return Math.max(0, Math.min(100, blended));
    }

    private void sleep(double speed) throws InterruptedException {
        long ms = (long) Math.max(60, tickIntervalMs / Math.max(0.25, speed));
        Thread.sleep(ms);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void appendInline(StringBuilder sb, String label, String value) {
        if (notBlank(value)) {
            sb.append(label).append(": ").append(value.trim().replaceAll("\\s+", " ")).append(". ");
        }
    }

    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String fmt(double v) {
        return String.format("$%,.0f", v);
    }

    private static String trimNumber(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }







}

