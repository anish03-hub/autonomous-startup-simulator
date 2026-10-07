package com.startupsimulator.config;

/**
 * Explicit AI execution mode, distinct from provider availability.
 *
 * <p>Phase 2A separated the {@code mock} and {@code openai} providers; Phase 2
 * separates <em>what the operator asked for</em> ({@code REAL} vs
 * {@code SCRIPTED_DEMO}) from <em>what the runtime can do</em>
 * ({@link LLMService#isRealProvider()}). The two are deliberately orthogonal:
 *
 * <ul>
 *   <li>{@link #REAL} — the agents must reason through a real LLM and their
 *       validated output becomes the authoritative startup state. If no real
 *       provider is available the run fails explicitly; it never silently
 *       fabricates reasoning from the deterministic scripts.</li>
 *   <li>{@link #SCRIPTED_DEMO} — the agents run their deterministic Phase 1
 *       scripts with <b>zero</b> LLM calls. This keeps demos and the whole test
 *       suite reproducible and free of any network/API dependency.</li>
 * </ul>
 *
 * <p>The default is {@link #SCRIPTED_DEMO} so an un-configured environment (and
 * every test) is offline-safe by construction.
 */
public enum AiExecutionMode {
    /** Agents reason through a real LLM; validated output is authoritative. */
    REAL,
    /** Deterministic scripted analysis, no LLM calls — demo/test safe. */
    SCRIPTED_DEMO
}
