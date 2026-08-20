---
name: False negative report
about: A native-image build failed or fell back, but the tool didn't flag the cause
title: "[False Negative] "
labels: false-negative
---

## What went wrong at native-image build time

<!-- Paste the relevant native-image error or fallback-image warning -->

## What the tool reported instead

<!-- Compatibility score / risk level / findings list from report.html or report.json -->

## The actual root cause

<!-- The specific reflective/proxy/JNI/serialization/class-loading pattern that caused the failure -->

## Minimal reproduction (if possible)

<!-- A small code snippet or sample project that reproduces the missed detection -->

## GraalVM version

---

Per docs/evaluation-plan.md, missed risks (false negatives) are
weighted as more costly than false positives, so these reports are
especially valuable for retuning ScoringWeights and, eventually,
Phase 2 training data.
