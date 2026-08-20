package io.nativerisk.core.model;

/**
 * Coarse risk bucket derived from the numeric compatibility score.
 * Thresholds live in ScoringWeights so they stay next to the rest of
 * the tunable scoring configuration.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
