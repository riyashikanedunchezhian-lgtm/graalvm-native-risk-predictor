package io.nativerisk.ml.features;

import io.nativerisk.core.model.FeatureVector;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;

/**
 * NOT YET IMPLEMENTED beyond this stub.
 *
 * Intended purpose: convert a core FeatureVector (counts by category
 * and severity) into a flat double[] suitable for Smile's Random
 * Forest input. Left unimplemented because there is no labeled
 * dataset to train against yet -- see native-risk-ml/README.md and
 * docs/data-sourcing.md. Implementing this before the dataset exists
 * would just be guessing at a feature representation with no way to
 * validate it.
 */
public final class FeatureVectorEncoder {

    public double[] encode(FeatureVector features) {
        throw new UnsupportedOperationException(
                "Phase 2 ML feature encoding is not implemented. "
                        + "See native-risk-ml/README.md: this is gated on a labeled dataset existing "
                        + "(docs/data-sourcing.md). Use HeuristicScoringEngine (native-risk-core) instead."
        );
    }
}
