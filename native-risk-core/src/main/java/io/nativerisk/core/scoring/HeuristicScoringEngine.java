package io.nativerisk.core.scoring;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.RiskLevel;

import java.util.List;

/**
 * Phase 1 scoring: a transparent, hand-tuned weighted-penalty model.
 * See ScoringWeights for where the numbers come from and their known
 * limitations. This is the always-available default -- Phase 2's ML
 * model (if built) is only used in its place if it demonstrably beats
 * this baseline on held-out evaluation data (see docs/evaluation-plan.md).
 */
public final class HeuristicScoringEngine {

    public int score(List<Finding> findings) {
        double totalPenalty = 0.0;
        for (Finding f : findings) {
            totalPenalty += ScoringWeights.penaltyFor(f.getCategory(), f.getSeverity());
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
