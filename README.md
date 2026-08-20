# GraalVM Native Image Compatibility Risk Predictor

A Gradle plugin that analyzes a Java project's compiled bytecode and
dependencies **before** you run `native-image`, surfacing likely
GraalVM Native Image compatibility issues early — reflection, dynamic
proxies, JNI, non-constant resource loading, serialization, custom
class loaders, and `invokedynamic`/`MethodHandle` usage.

Phase 1 (shipped) is a transparent, rule-based heuristic engine.
Phase 2 (conditional, not yet built) is an optional ML-based scoring
model, gated behind collecting a real labeled dataset of native-image
build outcomes. See [docs/proposal.md](docs/proposal.md) for the full
design rationale.

## Why

GraalVM Native Image performs ahead-of-time compilation under a
closed-world assumption: all reachable code must be knowable at build
time. Dynamic behavior (reflection, proxies, JNI, runtime class
loading) needs explicit configuration, and today developers usually
find out it's missing only after a native build fails — which can
take several minutes per attempt. This tool runs in seconds, right
after compilation, and gives you a prioritized list of what to fix
before you even attempt the native build.

## Quickstart

```bash
gradle nativeCompatibilityCheck
```

(No Gradle wrapper is checked into this repo yet — see CONTRIBUTING.md
to generate and commit one, after which this becomes `./gradlew ...`.)

This produces:

- Console summary with a Compatibility Score (0–100) and Low/Medium/High risk label
- `build/reports/native-risk/report.html` — human-readable report
- `build/reports/native-risk/report.json` — machine-readable report for CI gating

### Example output

```
Compatibility Score: 84/100   Risk: Medium   (heuristic engine)

Detected Issues:
  [HIGH]   Reflection call in com.app.Utils
  [MEDIUM] Dynamic proxy creation via Spring
  [LOW]    FileInputStream resource loading with variable path

Recommendations:
  - Add Utils to reflect-config.json
  - Register the relevant interfaces in proxy-config.json
  - Verify the resource path is available at build time, or pass --allow-incomplete-classpath
```

## Project layout

| Module | Purpose |
|---|---|
| `native-risk-core` | Framework-independent analysis engine (ASM bytecode inspection, ClassGraph classpath scanning, heuristic scoring, recommendations, reporting). No Gradle API dependency, so it's reusable from a future Maven plugin. |
| `native-risk-gradle-plugin` | Thin Gradle integration: hooks the core engine into the build lifecycle as a `nativeCompatibilityCheck` task. |
| `native-risk-ml` | Phase 2, optional and currently unbuilt. Random Forest scoring model, gated on a labeled dataset existing. See [native-risk-ml/README.md](native-risk-ml/README.md). |
| `benchmark` | Precision/recall evaluation harness comparing the heuristic engine (and later, the ML model) against real native-image build outcomes on a held-out project set. |
| `data` | Dataset labeling schema and mining scripts — not the dataset itself (see [data/labeling-schema.md](data/labeling-schema.md)). |
| `samples` | Small example Java projects used for manual testing and Gradle functional tests. |
| `docs` | Design docs: architecture, evaluation plan, data sourcing plan, known limitations. |

## Status

Phase 1 (heuristic engine) — in development, see module READMEs for current detector coverage.
Phase 2 (ML scoring) — not started; blocked on dataset collection (see [docs/data-sourcing.md](docs/data-sourcing.md)).

## Requirements

- Java 21+
- Gradle 8.7+ (wrapper included once you run `gradle wrapper` locally — see CONTRIBUTING.md)

## License

See [LICENSE](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Found a false positive or false
negative? Please use the issue templates under `.github/ISSUE_TEMPLATE/`
— this feedback directly informs heuristic tuning and, eventually,
Phase 2 training data.
