package io.nativerisk.core.model;

/**
 * How much weight a single Finding should carry when computing the
 * overall compatibility score. See {@code ScoringWeights} for how
 * these map to numeric penalties.
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH
}
