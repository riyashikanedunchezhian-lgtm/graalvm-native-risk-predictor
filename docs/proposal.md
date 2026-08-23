**GraalVM Native Image Compatibility Risk Predictor**

# Executive Summary

Modern Java frameworks such as Spring Boot, Quarkus, and Micronaut
increasingly rely on GraalVM Native Image to achieve fast startup and
reduced memory footprint. Native Image performs ahead-of-time
compilation under a *closed-world assumption*: all reachable code and
metadata must be knowable at build time. Dynamic behaviors such as
reflection, dynamic proxies, JNI calls, and runtime class loading
require explicit configuration, and developers typically discover
missing configuration only after a lengthy native build fails or the
resulting executable misbehaves.

This proposal describes a developer tool that analyzes a Java project\'s
compiled bytecode and dependencies before invoking native-image,
surfacing likely compatibility issues early. The initial deliverable is
a transparent, rule-based heuristic engine built on ASM (bytecode
inspection) and ClassGraph (classpath scanning), packaged as a Gradle
plugin with HTML and JSON reports. A machine-learning risk model is
treated as a well-scoped, optional Phase 2 enhancement, contingent on
assembling a labeled dataset of real build outcomes --- not as a
load-bearing component of the initial release.

This version of the proposal responds directly to feedback on an earlier
draft. In particular, it: (1) replaces an unsubstantiated ML-first
design with a heuristic-first, ML-later roadmap; (2) adds a concrete
data-sourcing and evaluation plan; (3) explicitly acknowledges the
static-analysis ceiling inherited from GraalVM\'s own reachability
limitations; (4) gives a fairer comparison against Quarkus and Spring
Boot AOT; and (5) removes citation formatting and analogies that could
not be independently verified.

# Problem Statement & Motivation

GraalVM Native Image offers substantial performance benefits ---
near-instant startup and a small runtime footprint --- which are
especially valuable for cloud-native and serverless Java workloads.
Adoption is hindered, however, by the closed-world analysis requirement:
any dynamically accessed class or member (via reflection, Class.forName,
serialization, dynamic proxies, or JNI) must be known in advance, or
explicitly registered via configuration.

GraalVM\'s own build-time analysis attempts to detect many reflective
and proxy usages automatically, and its tracing agent can record
additional metadata by observing a running application. Both are useful,
but both are reactive: the tracing agent requires executing the
application (and only as thoroughly as the exercised code paths allow),
and build-time detection only reports its findings once a native build
has already been attempted, which for larger applications can take many
minutes.

Framework-level tooling --- Spring Boot\'s AOT processing and Quarkus\'s
extension metadata --- meaningfully reduces this burden for
well-supported libraries, but is scoped to each framework\'s own
ecosystem and does not offer a general, pre-build compatibility estimate
for the wide range of libraries a typical enterprise project depends on.

The gap this proposal targets is narrow and specific: there is no
lightweight, framework-independent check that developers can run
immediately after compilation, before committing to a native build, to
get a rough sense of where their project is likely to run into trouble.

# Proposed Solution

The system is delivered as a Gradle plugin with two components, released
in phases so that the tool provides value even if the machine-learning
phase is never completed.

## Phase 1: Rule-Based Static Compatibility Checker (MVP)

-   **Bytecode Analyzer (ASM):** Parses compiled .class files to flag
    known-risky bytecode patterns --- calls to Class.forName,
    java.lang.reflect APIs, java.lang.reflect.Proxy construction, JNI
    method declarations, and getResourceAsStream calls with non-constant
    arguments.

-   **Dependency Scanner (ClassGraph):** Scans the classpath for
    annotations, resources, and libraries known from public GraalVM
    reachability-metadata repositories to require configuration (e.g.,
    certain ORMs, JSON libraries, and serialization frameworks).

-   **Heuristic Scoring Engine:** Combines the counts and categories of
    detected patterns into a weighted compatibility score (0--100) and a
    Low/Medium/High risk label, using transparent, hand-tuned weights
    that are documented and adjustable by the user. This avoids
    depending on a labeled training corpus that does not yet exist,
    while still giving developers an actionable, explainable signal.

-   **Recommendation Engine & Reporter:** Maps each detected issue to a
    specific, actionable fix (e.g., "add this class to
    reflect-config.json," "register this interface in
    proxy-config.json") and produces both an HTML report for developers
    and a JSON report for CI pipelines.

## Phase 2: Machine-Learning-Enhanced Scoring (Conditional, Post-MVP)

A Random Forest classifier (via the Smile library) may later replace or
augment the hand-tuned heuristic weights, but only once a genuine
labeled dataset exists. This phase is explicitly conditional and is
described in detail in the Data Sourcing and Evaluation sections below,
rather than assumed as a given in the initial architecture.

# Data Sourcing and Model Training Plan

The earlier draft of this proposal assumed the existence of "a corpus of
Java projects labeled by their native-image outcome" without describing
how such a corpus would be built. This is corrected here, since it is
the single largest risk to the ML component.

-   **Candidate sources:** Open-source Java repositories on GitHub that
    already use the GraalVM native-image Gradle/Maven plugin, filtered
    to those with CI logs showing native-image build attempts (success
    or failure) that can be legally mined under their existing licenses.

-   **Labeling method:** Where CI logs are available, the build outcome
    (success, failure, or fallback-image warning) is extracted directly
    rather than inferred. Where logs are unavailable, projects are
    excluded rather than guessed at.

-   **Known bias to correct for:** Public repositories that use
    native-image successfully are over-represented relative to projects
    that tried and abandoned it, since failing configurations are less
    likely to be published or are quietly fixed before merge. Any
    trained model must be evaluated with this selection bias explicitly
    reported, not ignored.

-   **Minimum viable dataset:** Phase 2 will not begin model training
    until at least several hundred labeled build outcomes, spanning a
    range of dependency profiles, have been collected; below that size,
    a Random Forest is unlikely to generalize and the heuristic engine
    remains the shipped default.

-   **Retraining and staleness:** Each GraalVM release tends to improve
    automatic detection of reflection and proxy usage, which can shift
    the meaning of "risk" over time. Any trained model will be versioned
    against the GraalVM release it was trained for and re-evaluated on
    each new GraalVM release rather than assumed to remain valid
    indefinitely.

# Known Limitations

This section states plainly what the tool cannot do, rather than
implying it solves the compatibility problem outright.

-   **Inherited static-analysis ceiling:** GraalVM\'s own closed-world
    limitation exists precisely because some dynamic behavior (e.g.,
    Class.forName called with a value built from user input or
    configuration) cannot be resolved without running the program. This
    tool inherits the same blind spot: it can flag the presence of a
    dynamic call, but generally cannot determine what class it will
    resolve to at runtime, or whether the flagged code path is even
    reachable in practice.

-   **False positives and false negatives are expected:** A conservative
    design (favoring false positives, i.e., flagging things that turn
    out to be fine) is preferred over one that under-reports risk, since
    a missed issue costs a developer a full native build cycle to
    discover, while a false positive costs only a few minutes of review.

-   **No substitute for the tracing agent:** The tracing agent remains
    the most reliable source of ground truth because it observes actual
    execution. This tool is positioned as an earlier, cheaper,
    complementary check --- not a replacement for the tracing agent or
    for a real native build in CI.

-   **Library coverage is incomplete:** The dependency scanner\'s
    knowledge of "known risky libraries" is only as good as the metadata
    it is seeded with, and will need ongoing maintenance as new library
    versions change their reflective behavior.

# System Architecture

The workflow, triggered after compilation, is:

-   1\. Java Project (compiled) → the plugin hooks into the Gradle build
    lifecycle after the compile task.

-   2\. Bytecode Analyzer (ASM) → inspects .class files for reflection,
    proxy, JNI, and resource-loading patterns.

-   3\. Dependency Scanner (ClassGraph) → scans the classpath for
    annotations, resources, and libraries with known GraalVM
    implications.

-   4\. Feature Extraction → detected patterns are compiled into a
    structured issue list and a feature vector.

-   5\. Scoring Engine → the heuristic engine (Phase 1) or trained
    Random Forest (Phase 2, if adopted) produces a compatibility score
    and risk level.

-   6\. Recommendation Engine → maps each issue to a specific,
    actionable fix.

-   7\. Gradle Plugin / Reporter → renders console output plus HTML and
    JSON reports, suitable for local development or CI gating.

# Key Technologies

-   **Java 21 (LTS):** target language version, aligned with current
    GraalVM support.

-   **ASM:** bytecode-level inspection without loading or executing
    classes; used to detect reflection, proxy, JNI, and resource-access
    instructions.

-   **ClassGraph:** fast classpath and module-path scanning for
    annotations, resources, and libraries, without initializing classes.

-   **Smile (Random Forest):** candidate library for the optional Phase
    2 ML scoring engine; chosen over deep-learning approaches for its
    interpretability, low resource requirements, and pure-JVM
    implementation --- but only pursued once a labeled dataset exists
    (see Data Sourcing).

-   **Gradle Plugin API:** integrates the checker into existing Java
    build workflows as a custom task (e.g., gradle
    nativeCompatibilityCheck).

-   **JUnit 5:** unit and integration testing of the analysis engine.

# Novelty and Comparison to Existing Solutions

The comparison below is intentionally more conservative than in the
original draft. Quarkus, in particular, already provides strong
reflection and resource detection for its supported extensions through
curated metadata, and Spring Boot AOT reduces many manual configuration
steps for Spring-managed beans. The genuine gap this tool fills is
framework-independent, pre-build risk estimation for arbitrary
dependency graphs --- not a wholesale replacement for either
ecosystem\'s tooling.

  ---------------------- --------------- ------------------- ---------------------- -------------------------
  **Feature**            **GraalVM       **Spring Boot AOT** **Quarkus**            **Proposed Tool**
                         Tracing Agent**                                            

  Pre-build static       No (runtime     Partial (AOT        Partial (extension     Yes, but limited to
  analysis               trace only)     processing)         metadata)              detectable patterns

  Reflection detection   Runtime-only,   Framework-scoped    Strong for known       Static pattern matching;
                         requires full   hints               extensions/libraries   misses
                         execution                                                  dynamically-constructed
                                                                                    strings

  Dependency/classpath   No              No                  Partial (extension     Yes (ClassGraph-based)
  scanning                                                   registry)              

  Compatibility risk     No              No                  No                     Yes (heuristic baseline;
  scoring                                                                           ML as optional
                                                                                    enhancement)

  Explainable            Low (raw logs)  Limited             Good for supported     Targeted, but not
  recommendations                                            extensions             exhaustive

  Requires running the   Yes             No                  No                     No
  application                                                                       

  Framework independence Yes             No                  No (Quarkus-specific)  Yes
                                         (Spring-specific)                          
  ---------------------- --------------- ------------------- ---------------------- -------------------------

The tool\'s real differentiators are narrower than "we are the only
solution that does X": it is framework-agnostic, it runs before any
native build is attempted, and it gives a single aggregate score
alongside per-issue detail. It does not claim to out-perform Quarkus\'s
extension-specific detection for libraries Quarkus already supports
well.

# Evaluation Plan

The original proposal had no stated success criteria. This version adds
one.

-   **Baseline comparison:** The heuristic engine (Phase 1) is evaluated
    on its own merits first: precision and recall of "flagged as risky"
    versus actual native-image build outcome, measured on a held-out set
    of open-source projects not used to tune the heuristic weights.

-   **ML comparison, if pursued:** A trained Random Forest (Phase 2) is
    only adopted in place of the heuristic engine if it demonstrably
    improves precision/recall over the Phase 1 baseline on the same
    held-out set; otherwise the simpler, more maintainable heuristic
    remains the default.

-   **Priority metric:** Recall on "will fail or require a fallback
    image" is prioritized over precision, since a missed risk costs a
    developer a full build cycle, while a false alarm costs a few
    minutes of review.

-   **Field feedback loop:** Developers using the tool can mark
    individual findings as false positives or false negatives from the
    HTML report; this feedback is logged locally and can optionally be
    aggregated to improve future heuristic tuning or training data.

# Expected Outputs and Contributions

When run on a project, the tool produces:

-   A Compatibility Score (0--100) indicating estimated readiness for a
    native build.

-   A Risk Classification (Low/Medium/High), with an explicit note on
    which detection method (heuristic or ML) produced it.

-   A ranked list of detected issues (e.g., "Reflection usage in
    com.example.MyService (Severity: High)").

-   Targeted, actionable recommendations for each issue.

-   An HTML report for developers and a machine-readable JSON report for
    CI pipelines.

Example Summary (from a hypothetical run):

**Compatibility Score:** 84/100 **Risk:** Medium *(heuristic engine)*

**Detected Issues:** Reflection call in com.app.Utils (High); Dynamic
proxy creation via Spring (Medium); FileInputStream resource loading
with variable path (Low).

**Recommendations:** Add Utils to reflect-config.json; register the
relevant interfaces in proxy-config.json; verify the resource path is
available at build time or pass \--allow-incomplete-classpath.

The project\'s realistic contributions are:

-   A transparent, pre-build static compatibility checker for GraalVM
    Native Image that does not depend on unproven ML assumptions to
    deliver value on day one.

-   A documented, versioned dataset and methodology for anyone who later
    wants to train a compatibility-prediction model, including an honest
    accounting of selection bias.

-   An explainable reporting format that pinpoints causes and fixes
    rather than just a pass/fail signal.

-   A reusable Gradle plugin, structured so a Maven plugin could reuse
    the same analysis core.

# Future Work

-   Maven and Bazel plugin support, sharing the same analysis core.

-   IDE integration (e.g., an IntelliJ IDEA plugin) surfacing issues at
    edit time.

-   Completion of the Phase 2 ML model, contingent on the data-sourcing
    plan above, with published precision/recall figures against the
    Phase 1 baseline.

-   Optional integration with an LLM to turn a detected issue into a
    longer natural-language explanation, clearly labeled as a
    convenience feature layered on top of, not a replacement for, the
    underlying static analysis.

-   A community feedback mechanism for reporting false
    positives/negatives, feeding back into heuristic tuning or future
    training data.

# Conclusion

The GraalVM Native Image Compatibility Risk Predictor addresses a real
and specific pain point: the late discovery of native-build
incompatibilities. This revised proposal delivers that value through a
transparent, rule-based static analyzer (ASM + ClassGraph) as its
initial, shippable contribution, and treats machine-learning-based
scoring as a clearly scoped, evidence-gated enhancement rather than an
assumed centerpiece. By stating its data-sourcing plan, its evaluation
criteria, and its inherited limitations up front, the project aims to be
a credible, incrementally-useful tool rather than an over-claimed one.

# References

This proposal draws on publicly available documentation and general
software-engineering literature, including: official GraalVM Native
Image documentation and reachability-metadata guidance; Spring Boot and
Quarkus native-image integration guides; the ASM and ClassGraph project
documentation; and general literature on Random Forest classifiers for
software analytics. Specific figures, quotations, and version-specific
claims should be verified against current documentation at build time,
since GraalVM\'s automatic detection capabilities are actively evolving.
