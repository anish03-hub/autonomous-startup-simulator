package com.startupsimulator.agent;

import java.util.List;
import java.util.Locale;

/**
 * Lightweight, deterministic heuristics that extract a few "insights" from a
 * free-text idea so that mock agent output feels tailored to the specific idea
 * rather than generic. This is intentionally simple; a real LLM would replace
 * it entirely.
 */
public final class IdeaAnalyzer {

    private IdeaAnalyzer() {
    }

    public static String audience(String idea) {
        String i = safe(idea);
        if (contains(i, "student", "college", "university", "campus")) return "college students";
        if (contains(i, "developer", "engineer", "devops")) return "software teams";
        if (contains(i, "small business", "smb", "startup", "founder")) return "early-stage founders and SMBs";
        if (contains(i, "enterprise", "b2b", "company", "companies")) return "mid-market and enterprise teams";
        if (contains(i, "parent", "family", "kid", "child")) return "busy parents and families";
        if (contains(i, "creator", "influencer", "content")) return "independent creators";
        if (contains(i, "patient", "health", "clinic", "medical")) return "health-conscious consumers";
        return "early adopters who feel this pain most acutely";
    }

    public static String domain(String idea) {
        String i = safe(idea);
        if (contains(i, "skincare", "beauty", "cosmetic")) return "beauty & personal care";
        if (contains(i, "fintech", "payment", "bank", "invest", "budget")) return "fintech";
        if (contains(i, "health", "fitness", "wellness", "medical")) return "health & wellness";
        if (contains(i, "education", "learn", "course", "tutor")) return "education";
        if (contains(i, "food", "recipe", "meal", "restaurant")) return "food & dining";
        if (contains(i, "travel", "trip", "hotel", "flight")) return "travel";
        if (contains(i, "shop", "ecommerce", "retail", "marketplace")) return "commerce";
        return "a fast-moving digital category";
    }

    public static String productNoun(String idea) {
        String i = safe(idea);
        if (contains(i, "platform")) return "platform";
        if (contains(i, "marketplace")) return "marketplace";
        if (contains(i, "app")) return "app";
        if (contains(i, "tool")) return "tool";
        if (contains(i, "assistant", "agent", "copilot")) return "AI assistant";
        return "product";
    }

    public static boolean isAiPowered(String idea) {
        return contains(safe(idea), "ai", "ml", "machine learning", "recommend", "predict", "smart", "intelligent");
    }

    /** A trimmed, single-line version of the idea for embedding in prose. */
    public static String oneLine(String idea) {
        String i = safe(idea).replaceAll("\\s+", " ").trim();
        if (i.length() > 160) {
            i = i.substring(0, 157) + "...";
        }
        return i.isEmpty() ? "the proposed product" : i;
    }

    private static boolean contains(String haystack, String... needles) {
        return List.of(needles).stream().anyMatch(haystack::contains);
    }

    private static String safe(String idea) {
        return idea == null ? "" : idea.toLowerCase(Locale.ROOT);
    }
}
