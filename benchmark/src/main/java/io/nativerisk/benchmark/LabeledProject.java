package io.nativerisk.benchmark;

/**
 * One entry in the held-out evaluation set: a project (by path or
 * git ref) and its ground-truth native-image build outcome, per the
 * labeling convention in docs/evaluation-plan.md.
 */
public record LabeledProject(
        String projectRef,
        Outcome outcome
) {
    public enum Outcome { CLEAN_SUCCESS, FALLBACK, FAILURE }

    /** True if this outcome counts as the positive ("risky") class. */
    public boolean isRisky() {
        return outcome == Outcome.FALLBACK || outcome == Outcome.FAILURE;
    }
}
