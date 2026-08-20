# Architecture

## Pipeline

The plugin hooks into the Gradle build lifecycle immediately after the
`compileJava` task and runs the following pipeline (implemented in
`native-risk-core`, orchestrated by `pipeline/AnalysisPipeline.java`):

```
1. Compiled .class files
        │
        ▼
2. Bytecode Analyzer (ASM)
   - visits every class file with ClassReader/ClassVisitor/MethodVisitor
   - each registered BytecodeDetector inspects instructions/annotations
   - produces a list of Finding objects
        │
        ▼
3. Dependency Scanner (ClassGraph)
   - scans the runtime classpath for annotations, resources, and
     libraries with known GraalVM reachability-metadata requirements
   - produces additional Finding objects
        │
        ▼
4. Feature Extraction
   - Findings are aggregated into a FeatureVector (counts per category,
     per severity) — this is the shared input to both scoring engines
        │
        ▼
5. Scoring Engine
   - Phase 1: HeuristicScoringEngine (weighted rules, always available)
   - Phase 2: ML model (io.nativerisk.ml), used only if present and
     if it demonstrably beats the heuristic baseline (see evaluation-plan.md)
        │
        ▼
6. Recommendation Engine
   - maps each Finding to a specific, actionable fix
        │
        ▼
7. Reporter
   - console summary
   - HTML report (build/reports/native-risk/report.html)
   - JSON report (build/reports/native-risk/report.json) for CI gating
```

## Module boundaries

`native-risk-core` has no dependency on the Gradle API or plugin SDK.
Everything Gradle-specific (task registration, build lifecycle hooks,
`Project` access) lives in `native-risk-gradle-plugin`, which depends
on core but not vice versa. This is what makes it feasible to build a
Maven or Bazel plugin later that reuses the same analysis engine — see
`docs/proposal.md`, Future Work.

`native-risk-ml` depends on core (for `Finding`/`FeatureVector` types)
but core never depends on it. The heuristic engine works standalone
whether or not the ML module is built or present on the classpath.

## Key classes

| Class | Module | Responsibility |
|---|---|---|
| `BytecodeDetector` (interface) | core | Contract for a single ASM-based pattern detector |
| `AnalysisPipeline` | core | Orchestrates steps 2–7 |
| `DependencyRiskScanner` | core | ClassGraph-based classpath scanning |
| `HeuristicScoringEngine` | core | Phase 1 scoring |
| `ScoringWeights` | core | Documented, adjustable weight table |
| `RecommendationEngine` | core | Finding → actionable fix mapping |
| `HtmlReportGenerator` / `JsonReportGenerator` | core | Output rendering |
| `NativeCompatibilityPlugin` / `NativeCompatibilityCheckTask` | gradle-plugin | Gradle integration |
