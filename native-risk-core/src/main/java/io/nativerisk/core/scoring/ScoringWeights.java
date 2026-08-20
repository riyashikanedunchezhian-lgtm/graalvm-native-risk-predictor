package io.nativerisk.core.scoring;

import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;

import java.util.EnumMap;
import java.util.Map;

/**
 * Documented, adjustable penalty weights for the Phase 1 heuristic
 * scoring engine. These are NOT derived from a labeled dataset --
 * Phase 2 is explicitly gated on that not existing yet (see
 * docs/data-sourcing.md). They are seeded from two sources:
 *
 *   1. Relative frequency of each pattern as a documented cause of
 *      native-image build failures in GraalVM's own troubleshooting
 *      guide and reachability-metadata repository issue history.
 *   2. Manual review of a small number (not a statistically
 *      meaningful sample) of known-problematic open-source projects
 *      during initial development.
 *
 * Because of (2), these weights should be treated as a reasonable
 * starting point, not a validated model. They are the first thing
 * that should be revisited once docs/evaluation-plan.md's baseline
 * evaluation produces real precision/recall data -- at that point,
 * weights should be adjusted based on which categories are actually
 * driving false positives/negatives, not intuition.
 *
 * The overall design deliberately favors recall over precision (see
 * evaluation-plan.md "Priority metric"): HIGH-severity findings and
 * categories that are hard to statically resolve (JNI, custom class
 * loaders) are weighted more heavily than lower-confidence signals
 * (invokedynamic, non-constant resource loads).
 */
public final class ScoringWeights {

    // Base penalty per Finding, by severity. Applied once per Finding.
    public static final Map<Severity, Integer> SEVERITY_PENALTY = new EnumMap<>(Severity.class);
    static {
        SEVERITY_PENALTY.put(Severity.LOW, 1);
        SEVERITY_PENALTY.put(Severity.MEDIUM, 3);
        SEVERITY_PENALTY.put(Severity.HIGH, 6);
    }

    // Category multiplier: how much extra weight a category gets beyond
    // its raw severity, reflecting how hard the category is to statically
    // resolve or auto-detect via GraalVM's own build-time analysis.
    public static final Map<FindingCategory, Double> CATEGORY_MULTIPLIER = new EnumMap<>(FindingCategory.class);
    static {
        CATEGORY_MULTIPLIER.put(FindingCategory.JNI, 1.5);                      // GraalVM cannot infer JNI needs at all
        CATEGORY_MULTIPLIER.put(FindingCategory.CUSTOM_CLASS_LOADER, 1.5);      // fundamentally hard to resolve statically
        CATEGORY_MULTIPLIER.put(FindingCategory.SERIALIZATION, 1.2);
        CATEGORY_MULTIPLIER.put(FindingCategory.REFLECTION, 1.0);               // GraalVM's own detection catches many cases
        CATEGORY_MULTIPLIER.put(FindingCategory.DYNAMIC_PROXY, 1.0);
        CATEGORY_MULTIPLIER.put(FindingCategory.DEPENDENCY_KNOWN_RISKY_LIBRARY, 0.8);
        CATEGORY_MULTIPLIER.put(FindingCategory.NON_CONSTANT_RESOURCE_LOAD, 0.6);
        CATEGORY_MULTIPLIER.put(FindingCategory.INVOKE_DYNAMIC, 0.5);           // lower-confidence signal, see detector docs
    }

    // Score starts at 100 and is reduced by penalties, floored at 0.
    public static final int STARTING_SCORE = 100;

    // Risk level thresholds on the final 0-100 score.
    public static final int HIGH_RISK_MAX_SCORE = 59;    // score <= this -> HIGH
    public static final int MEDIUM_RISK_MAX_SCORE = 84;  // score <= this (and > HIGH threshold) -> MEDIUM
    // anything above MEDIUM_RISK_MAX_SCORE -> LOW

    private ScoringWeights() {
    }

    public static double penaltyFor(FindingCategory category, Severity severity) {
        int base = SEVERITY_PENALTY.getOrDefault(severity, 1);
        double multiplier = CATEGORY_MULTIPLIER.getOrDefault(category, 1.0);
        return base * multiplier;
    }
}
