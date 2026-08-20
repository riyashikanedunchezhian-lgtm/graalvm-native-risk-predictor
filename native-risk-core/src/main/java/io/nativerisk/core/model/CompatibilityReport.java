package io.nativerisk.core.model;

import java.util.List;

/**
 * The full output of a single analysis run: score, risk level, all
 * findings, all recommendations, and which scoring engine produced
 * the score (heuristic vs. ML) -- surfaced explicitly per the
 * proposal's "Expected Outputs" section.
 */
public final class CompatibilityReport {

    public enum ScoringMethod { HEURISTIC, ML }

    private final int compatibilityScore; // 0-100
    private final RiskLevel riskLevel;
    private final ScoringMethod scoringMethod;
    private final List<Finding> findings;
    private final List<Recommendation> recommendations;

    public CompatibilityReport(int compatibilityScore,
                                RiskLevel riskLevel,
                                ScoringMethod scoringMethod,
                                List<Finding> findings,
                                List<Recommendation> recommendations) {
        this.compatibilityScore = compatibilityScore;
        this.riskLevel = riskLevel;
        this.scoringMethod = scoringMethod;
        this.findings = findings;
        this.recommendations = recommendations;
    }

    public int getCompatibilityScore() {
        return compatibilityScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public ScoringMethod getScoringMethod() {
        return scoringMethod;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }
}
