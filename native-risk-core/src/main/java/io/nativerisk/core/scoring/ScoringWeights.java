package io.nativerisk.core.scoring;
 
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
 
import java.util.EnumMap;
import java.util.Map;
 
/**
 * Documented, adjustable penalty model for the Phase 1 heuristic
 * scoring engine. These numbers are NOT derived from a labeled
 * dataset -- Phase 2 is explicitly gated on that not existing yet
 * (see docs/data-sourcing.md). They are seeded from three sources:
 *
 *   1. Relative frequency of each pattern as a documented cause of
 *      native-image build failures in GraalVM's own troubleshooting
 *      guide and reachability-metadata repository issue history.
 *   2. Manual review of a small number (not a statistically
 *      meaningful sample) of known-problematic open-source projects
 *      during initial development.
 *   3. Manual testing against the fixtures in samples/ during initial
 *      development, which surfaced and corrected a specific modeling
 *      flaw (see "Why per-category caps" below) before this ever saw
 *      real project code.
 *
 * Because of (1) and (2), these weights should be treated as a
 * reasonable starting point, not a validated model. They are the
 * first thing that should be revisited once docs/evaluation-plan.md's
 * baseline evaluation produces real precision/recall data -- at that
 * point, weights should be adjusted based on which categories are
 * actually driving false positives/negatives, not intuition.
 *
 * === Why per-category caps, not flat per-finding penalties ===
 *
 * An earlier version of this class charged a fixed penalty per
 * Finding (severity weight x category multiplier), added up across
 * every finding with no diminishing returns. That produced a scoring
 * inversion: a class with six ordinary REFLECTION findings (mostly
 * MEDIUM severity, individually cheap, and usually all fixed by one
 * reflect-config.json edit) scored WORSE than a class with two HIGH
 * findings in JNI and CUSTOM_CLASS_LOADER -- the two categories this
 * table explicitly considers hardest to fix, because GraalVM cannot
 * resolve them through configuration alone at all. That's backwards:
 * volume of an easy-to-fix pattern should not outrank a small number
 * of hard-to-fix ones.
 *
 * The fix: each category has a CATEGORY_CAP -- the maximum number of
 * points that category alone can ever cost, no matter how many
 * findings pile up in it. Findings within the same category approach
 * that cap with diminishing returns (the first finding in a category
 * costs much more than the fifth), while DIFFERENT categories still
 * stack independently and add up -- so a project with both a JNI
 * finding and a custom-class-loader finding is scored as more risky
 * than either alone, which matches the intuition that those are two
 * separate, compounding problems.
 *
 * The exact formula (see HeuristicScoringEngine):
 *
 *   weightedCount(category) = sum of SEVERITY_WEIGHT over every
 *                              finding in that category
 *   penalty(category) = CATEGORY_CAP(category)
 *                        x (1 - e^(-SATURATION_RATE x weightedCount))
 *   totalPenalty = sum of penalty(category) over every category
 *                  with at least one finding
 *   score = round(100 - totalPenalty), clamped to [0, 100]
 */
public final class ScoringWeights {
 
    // How much one Finding of a given severity contributes toward its
    // category's "weighted count" (see formula above). This is a
    // relative weight, not a standalone point deduction.
    public static final Map<Severity, Double> SEVERITY_WEIGHT = new EnumMap<>(Severity.class);
    static {
        SEVERITY_WEIGHT.put(Severity.LOW, 0.3);
        SEVERITY_WEIGHT.put(Severity.MEDIUM, 0.6);
        SEVERITY_WEIGHT.put(Severity.HIGH, 1.0);
    }
 
    // Maximum points a category can ever cost, regardless of how many
    // findings land in it. Reflects how hard the category is to fix
    // via configuration alone / how well GraalVM's own build-time
    // analysis already handles it without this tool's help.
    public static final Map<FindingCategory, Double> CATEGORY_CAP = new EnumMap<>(FindingCategory.class);
    static {
        CATEGORY_CAP.put(FindingCategory.JNI, 45.0);                       // GraalVM cannot infer JNI needs at all
        CATEGORY_CAP.put(FindingCategory.CUSTOM_CLASS_LOADER, 45.0);       // fundamentally hard to resolve statically
        CATEGORY_CAP.put(FindingCategory.SERIALIZATION, 25.0);
        CATEGORY_CAP.put(FindingCategory.REFLECTION, 18.0);                // GraalVM's own detection catches many cases;
                                                                            // usually one reflect-config.json fix covers
                                                                            // every finding in this category on a class
        CATEGORY_CAP.put(FindingCategory.DYNAMIC_PROXY, 18.0);
        CATEGORY_CAP.put(FindingCategory.DEPENDENCY_KNOWN_RISKY_LIBRARY, 12.0);
        CATEGORY_CAP.put(FindingCategory.NON_CONSTANT_RESOURCE_LOAD, 8.0);
        CATEGORY_CAP.put(FindingCategory.INVOKE_DYNAMIC, 6.0);             // lower-confidence signal, see detector docs
    }
 
    // Governs how quickly repeated findings within the SAME category
    // approach that category's cap. Higher = the first finding already
    // captures most of the cap and later ones in the same category add
    // little; lower = penalty grows closer to linearly with count.
    // 0.7 means a single HIGH-severity finding alone (weightedCount 1.0)
    // already captures about half of its category's cap.
    public static final double SATURATION_RATE = 0.7;
 
    // Score starts at 100 and is reduced by penalties, floored at 0.
    public static final int STARTING_SCORE = 100;
 
    // Risk level thresholds on the final 0-100 score.
    public static final int HIGH_RISK_MAX_SCORE = 59;    // score <= this -> HIGH
    public static final int MEDIUM_RISK_MAX_SCORE = 84;  // score <= this (and > HIGH threshold) -> MEDIUM
    // anything above MEDIUM_RISK_MAX_SCORE -> LOW
 
    private ScoringWeights() {
    }
 
    public static double severityWeight(Severity severity) {
        return SEVERITY_WEIGHT.getOrDefault(severity, 0.5);
    }
 
    public static double categoryCap(FindingCategory category) {
        return CATEGORY_CAP.getOrDefault(category, 15.0);
    }
}