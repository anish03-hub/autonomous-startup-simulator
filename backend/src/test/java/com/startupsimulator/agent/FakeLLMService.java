package com.startupsimulator.agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Deterministic in-test stand-in for a real LLM provider. Tests inject this to
 * exercise an agent's real-provider code path WITHOUT making any network call.
 *
 * <p>Configurable behaviour:
 * <ul>
 *   <li>{@code canned} — the structured response object returned by
 *       {@link #generateStructured}; when null a {@link CeoAnalysisException} is
 *       thrown to simulate an LLM/transport/parse failure. It is typed as
 *       {@link Object} so any agent's response DTO can be returned (CEO or a
 *       department agent).</li>
 *   <li>{@code sequence} — an ordered queue of responses returned one per
 *       {@link #generateStructured} call (Phase 2C: a multi-turn debate makes
 *       many structured calls). The {@link #FAILURE} sentinel makes that
 *       specific call throw, to exercise per-turn fallback. When the queue is
 *       exhausted the last response is reused so a test never runs dry.</li>
 *   <li>{@code real} — what {@link #isRealProvider()} reports (whether the agent
 *       takes the real-LLM branch at all).</li>
 * </ul>
 * It records the last <em>and</em> every system/user prompt so tests can assert
 * the full debate context was passed to each turn (Phase 2C §21).
 */
public class FakeLLMService implements LLMService {

    /**
     * Sentinel placed in a {@link #returningSequence} to make that particular
     * structured call fail (throw {@link CeoAnalysisException}), so a test can
     * verify a single failed debate turn falls back without crashing the rest.
     */
    public static final Object FAILURE = new Object();

    private final boolean real;
    private final Object canned;
    private final Queue<Object> sequence;
    private Object lastSequenced;

    public String lastSystemPrompt;
    public String lastUserPrompt;
    public int structuredCalls;
    public final List<String> systemPrompts = new ArrayList<>();
    public final List<String> userPrompts = new ArrayList<>();

    private FakeLLMService(boolean real, Object canned, Queue<Object> sequence) {
        this.real = real;
        this.canned = canned;
        this.sequence = sequence;
    }

    /** A real provider that returns the given canned analysis (any response DTO). */
    public static FakeLLMService returning(Object canned) {
        return new FakeLLMService(true, canned, null);
    }

    /**
     * A real provider that returns each response in turn, one per structured
     * call. Use {@link #FAILURE} for a call that should throw. When the sequence
     * is exhausted the final response is reused.
     */
    public static FakeLLMService returningSequence(Object... responses) {
        return new FakeLLMService(true, null, new LinkedList<>(List.of(responses)));
    }

    /** A real provider whose structured call always fails. */
    public static FakeLLMService failing() {
        return new FakeLLMService(true, null, null);
    }

    /** A mock (offline) provider — the agent never takes the real-LLM branch. */
    public static FakeLLMService offline() {
        return new FakeLLMService(false, null, null);
    }

    @Override
    public String complete(String system, String user) {
        return user == null ? "" : user.trim();
    }

    @Override
    public String generate(String system, String user) {
        return complete(system, user);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T generateStructured(String system, String user, Class<T> type) {
        structuredCalls++;
        lastSystemPrompt = system;
        lastUserPrompt = user;
        systemPrompts.add(system);
        userPrompts.add(user);

        Object response;
        if (sequence != null) {
            response = sequence.isEmpty() ? lastSequenced : sequence.poll();
            lastSequenced = response;
        } else {
            response = canned;
        }
        if (response instanceof Throwable t) {
            if (t instanceof RuntimeException re) throw re;
            throw new CeoAnalysisException("Simulated LLM failure", t);
        }
        if (response == null || response == FAILURE) {
            throw new CeoAnalysisException("Simulated LLM failure.");
        }
        return (T) response;
    }

    @Override
    public String provider() {
        return "fake";
    }

    @Override
    public boolean isRealProvider() {
        return real;
    }
}
