package io.nativerisk.benchmark;

import java.util.List;

/**
 * Evaluation harness skeleton for docs/evaluation-plan.md.
 *
 * NOT YET WIRED to a real held-out dataset -- there isn't one checked
 * into this repository (see data/labeling-schema.md and
 * docs/data-sourcing.md). This class documents the intended
 * computation so it's ready to run once:
 *   1. A held-out set of LabeledProject entries exists (ground truth
 *      build outcomes, collected per docs/data-sourcing.md).
 *   2. Each project has been run through AnalysisPipeline to get a
 *      predicted risk level.
 *
 * Usage once wired up:
 *   ./gradlew :benchmark:run --args="path/to/labeled-projects.json"
 */
public final class PrecisionRecallReport {

    public static void main(String[] args) {
        System.out.println("native-risk benchmark harness");
        System.out.println("No held-out labeled dataset is available yet -- see docs/data-sourcing.md.");
        System.out.println("This is a skeleton; wire computePrecisionRecall() to real (predicted, actual) pairs once data exists.");
    }

    /**
     * @param predictedRisky whether the heuristic (or ML) engine flagged the project as risky
     * @param actual         ground-truth labels, same order as predictedRisky
     */
    public static PrecisionRecall computePrecisionRecall(List<Boolean> predictedRisky, List<LabeledProject> actual) {
        if (predictedRisky.size() != actual.size()) {
            throw new IllegalArgumentException("predictedRisky and actual must be the same size");
        }

        int truePositive = 0, falsePositive = 0, falseNegative = 0, trueNegative = 0;

        for (int i = 0; i < actual.size(); i++) {
            boolean predicted = predictedRisky.get(i);
            boolean isActuallyRisky = actual.get(i).isRisky();

            if (predicted && isActuallyRisky) truePositive++;
            else if (predicted && !isActuallyRisky) falsePositive++;
            else if (!predicted && isActuallyRisky) falseNegative++;
            else trueNegative++;
        }

        double precision = truePositive + falsePositive == 0 ? 0.0 : (double) truePositive / (truePositive + falsePositive);
        double recall = truePositive + falseNegative == 0 ? 0.0 : (double) truePositive / (truePositive + falseNegative);

        return new PrecisionRecall(precision, recall, truePositive, falsePositive, falseNegative, trueNegative);
    }

    public record PrecisionRecall(
            double precision,
            double recall,
            int truePositive,
            int falsePositive,
            int falseNegative,
            int trueNegative
    ) {
    }
}
