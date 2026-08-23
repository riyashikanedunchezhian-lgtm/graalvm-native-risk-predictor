package io.nativerisk.ml.model;

/**
 * NOT YET IMPLEMENTED.
 *
 * Intended purpose: load a trained Random Forest model (versioned
 * against a specific GraalVM release, per docs/data-sourcing.md
 * "Retraining and staleness") and score a FeatureVector against it,
 * implementing the same interface shape as
 * io.nativerisk.core.scoring.HeuristicScoringEngine so the two are
 * interchangeable in the pipeline once Phase 2 is adopted.
 *
 * Blocked on RandomForestTrainer / a labeled dataset existing.
 */
public final class TrainedRiskModel {

    public TrainedRiskModel(String trainedForGraalVmVersion) {
        throw new UnsupportedOperationException(
                "Phase 2 model loading is not implemented — no trained model exists yet. "
                        + "See docs/data-sourcing.md and native-risk-ml/README.md."
        );
    }
}
