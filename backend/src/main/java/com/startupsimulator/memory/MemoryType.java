package com.startupsimulator.memory;

/**
 * Phase 5A: the small, closed vocabulary describing <em>what kind</em> of durable
 * knowledge a memory captures. Deliberately a handful of broad categories (not
 * dozens): the point is a stable, filterable dimension, not a taxonomy.
 *
 * <p>Persisted with {@link jakarta.persistence.EnumType#STRING} so the stored
 * value is the readable name, resilient to reordering.
 */
public enum MemoryType {

    /** A choice that was made and should stay settled (e.g. "chose a modular monolith"). */
    DECISION,

    /** A durable factual statement about the startup/domain (e.g. "target users are dermatology clinics"). */
    FACT,

    /** A non-obvious realisation drawn from reasoning (e.g. "scanner accuracy drives retention"). */
    INSIGHT,

    /** Something learned, often from a failure or correction, worth not repeating. */
    LESSON,

    /** A risk worth remembering so later reasoning accounts for it. */
    RISK,

    /** A standing preference/constraint to honour in future work (e.g. "keep infra cost under $2k/mo"). */
    PREFERENCE
}
