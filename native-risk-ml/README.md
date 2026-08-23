# native-risk-ml (Phase 2 — not built)

This module is scaffolding only. Per `docs/data-sourcing.md`, Phase 2
(ML-based scoring) does not begin real implementation until a labeled
dataset of at least several hundred native-image build outcomes has
been collected. **That dataset does not exist yet.**

What's here is the intended shape of the module so the eventual
integration point with `native-risk-core` is clear:

- `features/` — will convert `io.nativerisk.core.model.FeatureVector`
  into whatever numeric feature representation Smile's Random Forest
  expects.
- `training/` — will contain the training pipeline, consuming labeled
  data per the schema in `data/labeling-schema.md`.
- `model/` — will contain trained-model loading and inference, with
  each model version tagged to the GraalVM release it was trained
  against (see `docs/data-sourcing.md`, "Retraining and staleness").

## Do not wire this into the Gradle plugin yet

The heuristic engine (`native-risk-core`) is the shipped default and
should remain so until a trained model demonstrably beats it on the
held-out evaluation set defined in `docs/evaluation-plan.md`. Wiring
this module into `native-risk-gradle-plugin` before that comparison
exists would contradict the whole point of gating Phase 2 on evidence.
