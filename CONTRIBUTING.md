# Contributing

## Setup

This repo does not yet have a Gradle wrapper checked in. Generate one
locally with a Gradle 8.7+ install, then commit it so CI and other
contributors don't need Gradle installed separately:

```bash
git clone <this repo>
cd graalvm-native-risk-predictor
gradle wrapper --gradle-version 8.7   # generates gradlew, gradlew.bat, gradle/wrapper/
git add gradlew gradlew.bat gradle/wrapper
./gradlew build
```

Until that's done, use your local `gradle` install directly (matching
what `.github/workflows/ci.yml` currently does):

```bash
gradle build
```

## Where things live

- Add a new bytecode detection pattern → `native-risk-core/.../analyzer/`, implement `BytecodeDetector`, register it in `pipeline/AnalysisPipeline.java`, add a test under `native-risk-core/src/test/.../analyzer/`.
- Add a new known-risky library signature → `native-risk-core/.../scanner/DependencyRiskScanner.java` (or the metadata file it loads, once that's externalized).
- Adjust heuristic scoring weights → `native-risk-core/.../scoring/ScoringWeights.java`. Document *why* a weight changed in the commit message; weights are meant to be auditable.
- Gradle task behavior → `native-risk-gradle-plugin/`.

## Tests

```bash
./gradlew test                 # unit tests, all modules
./gradlew :native-risk-gradle-plugin:functionalTest   # Gradle TestKit tests against samples/
```

Every new detector needs at least one positive test (pattern is flagged)
and one negative test (similar-looking but safe code is *not* flagged).

## Reporting false positives / false negatives

Please use the issue templates rather than a blank issue — they capture
the specific bytecode/dependency context we need to fix or tune
detection, and (per the proposal's field feedback loop) feed into
future heuristic tuning and Phase 2 training data.

## Code style

Standard Java conventions. No enforced formatter yet — keep diffs
minimal and consistent with surrounding code.
