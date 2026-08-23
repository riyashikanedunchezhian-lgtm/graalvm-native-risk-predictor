package io.nativerisk.core.model;

/**
 * A concrete, actionable fix mapped to one Finding. Kept separate
 * from Finding itself so the recommendation text can evolve (e.g.
 * localized, or made more specific with project context) without
 * touching detection logic.
 */
public final class Recommendation {

    private final Finding finding;
    private final String actionableFix;

    public Recommendation(Finding finding, String actionableFix) {
        this.finding = finding;
        this.actionableFix = actionableFix;
    }

    public Finding getFinding() {
        return finding;
    }

    public String getActionableFix() {
        return actionableFix;
    }
}
