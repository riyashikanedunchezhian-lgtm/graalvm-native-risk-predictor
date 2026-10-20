# GraalVM Native Image Compatibility Risk Predictor

---

## Overview

The **GraalVM Native Image Compatibility Risk Predictor** is a Gradle plugin that performs proactive analysis of Java bytecode and project dependencies **before** invoking `native-image`. It detects patterns that are known to cause compatibility issues with GraalVM's native‑image tool and provides concrete, actionable recommendations for remediation.

### Why this matters

* **Speed up CI/CD** – Catch native‑image failures early (seconds after compilation) instead of after a multi‑minute build.
* **Reduce debugging time** – Detailed findings point directly to the offending code and suggest the exact configuration changes needed.
* **Future‑proof** – The architecture is split between a core analysis engine (`native-risk-core`) and a thin Gradle integration layer (`native-risk-gradle-plugin`). This makes the engine reusable in other contexts (Maven, custom tooling, IDE plugins).

---

## Features

| Feature | Description |
|---|---|
| **Bytecode detectors** | Detects reflection, dynamic proxy, JNI, non‑constant resource loading (both `Class.forName` and `ResourceBundle.getBundle`), serialization, custom class loaders, `invokedynamic`, unsafe usage (`sun.misc.Unsafe`), service‑loader patterns, and known risky third‑party libraries. |
| **Heuristic scoring** | Assigns a compatibility score (0‑100) with per‑category caps to avoid a single issue type dominating the result. |
| **Detailed report** | Generates a human‑readable HTML report and a machine‑readable JSON report (suitable for CI pipelines). |
| **Gradle task** | `nativeCompatibilityCheck` integrates seamlessly into the Gradle build lifecycle. |
| **Extensible architecture** | New detectors can be added by implementing the `BytecodeDetector` interface. |
| **Publishing support** | Plugin can be published to Maven Local, a corporate Nexus/Artifactory, or any remote Maven repository for sharing across teams. |

---

## Project Layout

| Module | Purpose |
|---|---|
| `native-risk-core` | Core analysis engine (ASM bytecode inspection, ClassGraph classpath scanning, heuristic scoring, recommendation generation, report creation). No Gradle API dependency – can be reused from Maven plugins, CLI tools, or IDE extensions. |
| `native-risk-gradle-plugin` | Thin Gradle plugin that wires the core engine into the build lifecycle (`nativeCompatibilityCheck` task). Includes publishing configuration for Maven Local/remote repositories. |
| `native-risk-ml` | Placeholder for Phase 2 – an optional ML‑based scoring model that will be trained on a labelled dataset of native‑image build outcomes. |
| `benchmark` | Evaluation harness to measure precision/recall of the heuristic engine (and later the ML model) against a held‑out set of real projects. |
| `data` | Schema and scripts for dataset labeling (no actual data is stored in the repo). |
| `samples` | Small example projects used for functional tests and documentation examples (`clean-spring-boot-app`, `known-fallback-case`, `reflection-heavy-app`). |
| `docs` | Design documentation, architecture diagrams, data‑sourcing plan, and known limitations. |

---

## Getting Started

### Prerequisites

* **Java 21+** – Required for compilation and for running the plugin. Verify with `java -version`.
* **Gradle 8.7+** – The project ships a Gradle wrapper (`./gradlew`). No separate Gradle installation is needed.

### Building the Project

```bash
# Clone the repository (if you haven't already)
git clone <repo‑url>
cd graalvm-native-risk-predictor

# Build all modules and run unit tests
./gradlew clean build
```

The above command compiles the core engine, the Gradle plugin, and executes all unit tests located under `native-risk-core/src/test/java` and functional tests under `native-risk-gradle-plugin/src/functionalTest/java`.

---

## Using the Plugin

### Method 1: Run on Sample Projects (Recommended for testing)

The samples are standalone Gradle builds that use **Composite Build** to pull in the plugin from source. **You must run from inside the sample directory.**

```powershell
# 1. Navigate to a sample (e.g., reflection-heavy-app)
cd samples\reflection-heavy-app

# 2. Set JDK (required on Windows)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"

# 3. Run the check using the ROOT wrapper (two levels up)
..\..\gradlew nativeCompatibilityCheck --no-daemon

# 4. View the HTML report
Start-Process .\build\reports\native-risk\report.html
```

Repeat for other samples in `samples\clean-spring-boot-app` and `samples\known-fallback-case`.

### Method 2: Publish Locally & Apply to Any Project

If you want to use the plugin in your own project without the composite build setup:

```powershell
# 1. From the ROOT project directory, publish to local Maven cache
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\gradlew :native-risk-gradle-plugin:publishToMavenLocal --no-daemon
```

Then in **your target project's** `build.gradle.kts`:

```kotlin
plugins {
    // Apply the published plugin
    id("io.nativerisk.native-compatibility")
}
```

Run the check in your project:
```powershell
./gradlew nativeCompatibilityCheck
```

Reports will appear under `<your-project>/build/reports/native-risk/`.

---

## Scoring Mechanism (Updated)

The heuristic engine now uses **per‑category caps** instead of a flat per‑finding penalty. Each `FindingCategory` has a maximum weight, preventing a single category (e.g., dozens of reflection calls) from skewing the overall score. The caps are defined in `native-risk-core/src/main/java/io/nativerisk/core/scoring/ScoringWeights.java`.

---

## Recommendation Engine

`RecommendationEngine` maps each `Finding` to a concise, actionable fix. Recent enhancements added support for the newly introduced `UNSAFE` and `SERVICE_LOADER` categories:

* **UNSAFE** – "Avoid using `sun.misc.Unsafe`; refactor to `java.lang.invoke.VarHandle` or safer APIs."
* **SERVICE_LOADER** – "Ensure `META-INF/services` files are present and listed in the GraalVM service‑loader config."

The engine can be extended by adding new `case` branches for additional `FindingCategory` values.

---

## Adding New Detectors

1. **Create a detector class** implementing `BytecodeDetector` (see existing detectors in `native-risk-core/src/main/java/io/nativerisk/core/analyzer`).
2. **Register the detector** in `AnalysisPipeline` construction (usually via `ServiceLoader` or manual list). 
3. **Add a `FindingCategory` entry** in `FindingCategory.java` if a new risk type is introduced.
4. **Update `ScoringWeights`** to assign a weight and optional per‑category cap.
5. **Add unit tests** under `native-risk-core/src/test/java/...` to verify detection.

---

## Publishing the Plugin Across Machines/Teams

### Local Maven Repository (shared drive or network share)

```bash
./gradlew :native-risk-gradle-plugin:publishToMavenLocal
```

Consumers only need to add the plugin ID (`io.nativerisk.native-compatibility`) – Gradle will locate it in `~/.m2/repository`.

### Remote Maven Repository (Nexus, Artifactory, GitHub Packages, etc.)

1. **Configure the repository** in `native-risk-gradle-plugin/build.gradle.kts`:

```kotlin
publishing {
    repositories {
        maven {
            name = "myCompanyRepo"
            url = uri("https://repo.mycompany.com/maven")
            credentials {
                username = findProperty("repoUser") as String?
                password = findProperty("repoPassword") as String?
            }
        }
    }
    publications {
        create<MavenPublication>("plugin") {
            from(components["java"])
        }
    }
}
```

2. **Publish**:

```bash
./gradlew :native-risk-gradle-plugin:publish
```

3. **Consume** in a downstream project:

```kotlin
repositories {
    maven { url = uri("https://repo.mycompany.com/maven") }
    mavenCentral()
}

plugins {
    id("io.nativerisk.native-compatibility") version "0.1.0-SNAPSHOT"
}
```

---

## Development Workflow

* **Run unit tests:** `./gradlew test`
* **Run functional tests (Gradle plugin integration):** `./gradlew functionalTest`
* **Generate the HTML report without publishing:** `./gradlew nativeCompatibilityCheck`
* **Publish a new snapshot:** Increment `version` in `build.gradle.kts` (e.g., `0.1.1-SNAPSHOT`) and run the publish task again.

### IDE Setup (IntelliJ IDEA / VS Code)

1. Import the root `build.gradle.kts` as a Gradle project.
2. Ensure the Java toolchain (Java 21) is configured in the IDE.
3. Run Gradle tasks directly from the IDE's Gradle tool window.

---

## Contributing

We welcome contributions! Please read the guidelines below before submitting a pull request.

1. **Fork the repository** and create a feature branch.
2. **Add or modify detectors** – ensure you include unit tests covering both positive and negative cases.
3. **Run the full build** (`./gradlew clean build`) and verify that no tests fail.
4. **Commit** with the following footer (required by the project's contribution policy):
   ```
   Co-Authored-By: Your Name <your.email@example.com>
   ```
5. **Open a pull request** – link any related JIRA tickets and fill out the PR template.

### Issue Templates

* `bug_report.md` – for regression bugs or detector inaccuracies.
* `feature_request.md` – for new detector ideas or enhancements to the scoring model.
* `false_positive_report.md` – for cases where a detector flags safe code.

---

## License

This project is licensed under the Apache License, Version 2.0. See the `LICENSE` file for details.

---

## Acknowledgments

* **GraalVM Team** – for the native‑image technology.
* **ASM library** – for bytecode manipulation.
* **ClassGraph** – for classpath scanning.
* **Open‑source contributors** – for bug reports, documentation improvements, and detector ideas.

---

## Roadmap (High‑Level)

| Milestone | Description |
|---|---|
| **Phase 1** (completed) | Heuristic engine with 10 detectors, per‑category caps, recommendation engine. |
| **Phase 2** (in progress) | ML‑based scoring model trained on a curated dataset of native‑image successes/failures. |
| **IDE plugins** | Provide IntelliJ/VS Code plugins that surface findings directly in the editor. |
| **Maven plugin** | Publish a Maven extension for projects that use Maven instead of Gradle. |
| **CI integrations** | Official GitHub Actions and GitLab CI templates for automated compliance checks. |

---

## Contact & Support

* **Issue tracker:** Use the GitHub Issues page.

---

*End of README*
