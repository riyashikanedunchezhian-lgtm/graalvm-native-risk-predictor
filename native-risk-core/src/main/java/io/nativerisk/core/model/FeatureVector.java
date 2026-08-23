package io.nativerisk.core.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated counts of Findings by category and severity, derived
 * from a full set of Findings for a project. This is the shared input
 * to both the Phase 1 heuristic engine and (if built) the Phase 2 ML
 * model -- it exists so the two scoring approaches consume exactly
 * the same signal and can be fairly compared during evaluation.
 */
public final class FeatureVector {

    private final Map<FindingCategory, Integer> countsByCategory = new EnumMap<>(FindingCategory.class);
    private final Map<Severity, Integer> countsBySeverity = new EnumMap<>(Severity.class);
    private final int totalFindings;

    public FeatureVector(List<Finding> findings) {
        for (FindingCategory c : FindingCategory.values()) {
            countsByCategory.put(c, 0);
        }
        for (Severity s : Severity.values()) {
            countsBySeverity.put(s, 0);
        }
        for (Finding f : findings) {
            countsByCategory.merge(f.getCategory(), 1, Integer::sum);
            countsBySeverity.merge(f.getSeverity(), 1, Integer::sum);
        }
        this.totalFindings = findings.size();
    }

    public int countFor(FindingCategory category) {
        return countsByCategory.getOrDefault(category, 0);
    }

    public int countFor(Severity severity) {
        return countsBySeverity.getOrDefault(severity, 0);
    }

    public int getTotalFindings() {
        return totalFindings;
    }
}
