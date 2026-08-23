package io.nativerisk.core.scoring;
 
import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.RiskLevel;
 
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
 
/**
 * Phase 1 scoring: a transparent, hand-tuned model using per-category
 * penalty caps with diminishing returns for repeated findings in the
 * same category (see ScoringWeights for the full rationale and the
 * scoring inversion this design specifically corrects). This is the
 * always-available default -- Phase 2's ML model (if built) is only
 * used in its place if it demonstrably beats this baseline on
 * held-out evaluation data (see docs/evaluation-plan.md).
 */
public final class HeuristicScoringEngine {
 
    public int score(List<Finding> findings) {
        // Step 1: sum each finding's severity weight into its category's
        // running total ("weighted count" in ScoringWeights' terminology).
        Map<FindingCategory, Double> weightedCountByCategory = new EnumMap<>(FindingCategory.class);
        for (Finding f : findings) {
            weightedCountByCategory.merge(
                    f.getCategory(),
                    ScoringWeights.severityWeight(f.getSeverity()),
                    Double::sum
            );
        }
 
        // Step 2: convert each category's weighted count into a penalty
        // that approaches (but never exceeds) that category's cap.
        double totalPenalty = 0.0;
        for (Map.Entry<FindingCategory, Double> entry : weightedCountByCategory.entrySet()) {
            FindingCategory category = entry.getKey();
            double weightedCount = entry.getValue();
            double cap = ScoringWeights.categoryCap(category);
 
            double penalty = cap * (1.0 - Math.exp(-ScoringWeights.SATURATION_RATE * weightedCount));
            totalPenalty += penalty;
        }
 
        int score = (int) Math.round(ScoringWeights.STARTING_SCORE - totalPenalty);
        return Math.max(0, Math.min(100, score));
    }
 
    public RiskLevel riskLevelFor(int score) {
        if (score <= ScoringWeights.HIGH_RISK_MAX_SCORE) {
            return RiskLevel.HIGH;
        } else if (score <= ScoringWeights.MEDIUM_RISK_MAX_SCORE) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
}
 