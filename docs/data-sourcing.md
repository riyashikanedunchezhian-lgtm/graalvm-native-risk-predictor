# Data Sourcing and Model Training Plan (Phase 2)

This corrects an earlier draft's assumption that "a corpus of Java
projects labeled by their native-image outcome" already exists. It
does not. This document describes how it would be built, and states
plainly that Phase 2 does not start until it exists at sufficient scale.

## Candidate sources

Open-source Java repositories on GitHub that already use the GraalVM
`native-image` Gradle or Maven plugin, filtered to those with CI logs
showing native-image build attempts (success, fallback, or failure)
that can be legally mined under their existing licenses. Only
repositories with a license that permits this kind of analysis are
included; license compatibility is checked and recorded per project,
not assumed.

## Labeling method

See `evaluation-plan.md` for the exact label definitions
(CLEAN_SUCCESS / FALLBACK / FAILURE). Where CI logs are available, the
outcome is extracted directly from log output, not inferred from
indirect signals (e.g., "the PR was merged" is not treated as evidence
of a successful native build). Where logs are unavailable or
ambiguous, the project is **excluded**, not guessed at.

## Known bias to correct for

Public repositories that use native-image successfully are
over-represented relative to projects that tried and abandoned it —
failing configurations are less likely to be published, or are
quietly fixed before merge and never show up as a "failure" in
history. Any trained model must be evaluated with this selection bias
explicitly reported (e.g., stated base rates of each label in the
dataset), not ignored or averaged away.

## Minimum viable dataset

Phase 2 training does not begin until at least several hundred labeled
build outcomes, spanning a range of dependency profiles (web
frameworks, ORMs, serialization libraries, messaging clients, etc.),
have been collected. Below that size, a Random Forest is unlikely to
generalize, and the heuristic engine remains the shipped default
regardless of how much engineering effort has gone into the ML
pipeline.

## Retraining and staleness

Each GraalVM release tends to improve automatic detection of
reflection and proxy usage, which shifts what "risk" means over time
(a pattern that required manual config in GraalVM 23 might be
auto-detected in GraalVM 25). Any trained model is versioned against
the GraalVM release it was trained for, and is re-evaluated — not
assumed to remain valid — on each new GraalVM release.
