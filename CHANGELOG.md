# Changelog

All notable changes to this project are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added
- Initial repository scaffold: `native-risk-core`, `native-risk-gradle-plugin`, `native-risk-ml` (stub), `benchmark`.
- Phase 1 bytecode detectors: reflection, dynamic proxy, JNI, non-constant resource loading, serialization, custom class loader, invokedynamic/MethodHandle.
- ClassGraph-based dependency risk scanner.
- Heuristic scoring engine with documented, adjustable weights.
- HTML and JSON report generation.
- `nativeCompatibilityCheck` Gradle task.
- Data sourcing plan and labeling schema (docs only; no dataset collected yet).
