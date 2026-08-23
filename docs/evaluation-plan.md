# Evaluation Plan

The original draft of this proposal had no stated success criteria.
This document fixes that, and defines terms precisely enough to make
the eventual precision/recall numbers reproducible.

## Outcome labels (used consistently across benchmark and Phase 2 data)

Every evaluated project run is labeled with exactly one of:

- **CLEAN_SUCCESS** — native-image build succeeds with no fallback warnings.
- **FALLBACK** — native-image build "succeeds" but emits a fallback image
  (i.e., it falls back to including a JVM, losing most AOT benefits).
- **FAILURE** — native-image build fails outright.

For precision/recall purposes, **FALLBACK and FAILURE are both treated
as "risky" (positive class)**; only CLEAN_SUCCESS is treated as
"not risky" (negative class). This is a judgment call — a fallback
image is not a hard failure, but it defeats the purpose of using
native-image at all, so treating it as a miss is the more useful
definition for this tool's audience. If this convention changes, all
reported precision/recall numbers must be re-labeled and re-reported;
they are not compatible across labeling conventions.

## Baseline comparison (Phase 1 — heuristic engine)

- Evaluated standalone, before any ML work begins.
- Metric: precision and recall of "flagged as risky" (Compatibility
  Score below a documented threshold, or any High-severity Finding)
  versus actual build outcome (per the labels above).
- Measured on a held-out set of open-source projects **not** used to
  set or tune the heuristic weights, to avoid overfitting the weights
  to the evaluation set.

## ML comparison (Phase 2, if pursued)

- A trained Random Forest is only adopted in place of, or alongside,
  the heuristic engine if it demonstrably improves precision/recall
  over the Phase 1 baseline **on the same held-out set**.
- If it does not clear that bar, the heuristic engine remains the
  shipped default — Phase 2 is not adopted for its own sake.

## Priority metric

**Recall on "will fail or fallback" is prioritized over precision.**
Rationale: a missed risk costs a developer a full native-image build
cycle to discover (potentially many minutes); a false alarm costs a
few minutes of review. This asymmetry means the heuristic weights
(see `ScoringWeights.java`) are deliberately tuned to over-flag rather
than under-flag when evidence is ambiguous.

## Field feedback loop

The HTML report includes controls for developers to mark individual
findings as false positive or false negative. This is logged locally
(not transmitted anywhere by default) and can optionally be
aggregated by a project/org to:
- retune heuristic weights, or
- contribute to Phase 2 training data (subject to the labeling
  convention above and the licensing constraints in `data-sourcing.md`).
