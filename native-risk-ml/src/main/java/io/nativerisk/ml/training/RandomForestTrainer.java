package io.nativerisk.ml.training;

/**
 * NOT YET IMPLEMENTED.
 *
 * Intended purpose: train a Smile Random Forest classifier on labeled
 * (FeatureVector, outcome) pairs, following the labeling convention
 * in docs/evaluation-plan.md (CLEAN_SUCCESS / FALLBACK / FAILURE,
 * with FALLBACK+FAILURE treated as the positive/risky class).
 *
 * Blocked on: docs/data-sourcing.md "Minimum viable dataset" --
 * several hundred labeled build outcomes across a range of dependency
 * profiles. Do not implement training logic against synthetic or
 * placeholder data; it would produce a model with no evaluative
 * meaning and risks being mistaken for something validated.
 */
public final class RandomForestTrainer {

    public RandomForestTrainer() {
        throw new UnsupportedOperationException(
                "Phase 2 training is not implemented — no labeled dataset exists yet. "
                        + "See docs/data-sourcing.md."
        );
    }
}
