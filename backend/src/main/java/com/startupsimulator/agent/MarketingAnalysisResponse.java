package com.startupsimulator.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strongly-typed Marketing analysis, produced as JSON structured output by the
 * LLM (or deterministically by the fallback). Marketing builds on the CEO's
 * direction and the Development scope to define positioning, competitor view,
 * pricing, channels and go-to-market; validated before it is written to state.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketingAnalysisResponse(
        String positioning,
        String competitorAnalysis,
        String pricingStrategy,
        List<String> channels,
        String goToMarket,
        String targetAudience,
        String valueProposition,
        String businessModel,
        List<String> risks,
        /**
         * Phase 5B: optional, agent-driven memory-creation intents (e.g. a durable
         * market insight or a positioning constraint worth remembering). Empty/absent
         * when nothing is worth recording. Persisted — bounded, validated,
         * de-duplicated — only after a successful analysis, through MemoryService.
         */
        List<AgentMemoryIntent> memoryIntents
) {

    /**
     * Legacy constructor (pre-Phase-5B arity) with no memory intents. Keeps the
     * existing single-constructor call-sites and tests compiling unchanged;
     * Jackson still binds the canonical constructor.
     */
    public MarketingAnalysisResponse(
            String positioning,
            String competitorAnalysis,
            String pricingStrategy,
            List<String> channels,
            String goToMarket,
            String targetAudience,
            String valueProposition,
            String businessModel,
            List<String> risks) {
        this(positioning, competitorAnalysis, pricingStrategy, channels, goToMarket,
                targetAudience, valueProposition, businessModel, risks, List.of());
    }

    /** Require the core narrative fields plus at least one acquisition channel. */
    public boolean isValid() {
        return notBlank(positioning)
                && notBlank(pricingStrategy)
                && notBlank(goToMarket)
                && channels != null && channels.stream().anyMatch(MarketingAnalysisResponse::notBlank);
    }

    public MarketingAnalysisResponse normalized() {
        return new MarketingAnalysisResponse(
                trim(positioning),
                trim(competitorAnalysis),
                trim(pricingStrategy),
                orEmpty(channels),
                trim(goToMarket),
                trim(targetAudience),
                trim(valueProposition),
                trim(businessModel),
                orEmpty(risks),
                orEmptyIntents(memoryIntents));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static List<String> orEmpty(List<String> list) {
        return list == null ? List.of()
                : list.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
    }

    private static List<AgentMemoryIntent> orEmptyIntents(List<AgentMemoryIntent> list) {
        return list == null ? List.of()
                : list.stream().filter(i -> i != null && i.isPresent()).toList();
    }
}
