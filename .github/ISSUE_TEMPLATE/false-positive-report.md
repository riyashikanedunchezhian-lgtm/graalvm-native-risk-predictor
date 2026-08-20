---
name: False positive report
about: The tool flagged something that turned out to be fine
title: "[False Positive] "
labels: false-positive
---

## What was flagged

<!-- Paste the relevant line(s) from report.html / report.json, including category, severity, and location -->

## Why it's not actually a risk

<!-- e.g. "the reflective call target is a compile-time constant that GraalVM's own build-time analysis already resolves" -->

## Minimal reproduction (if possible)

<!-- A small code snippet or sample project that reproduces the false flag -->

## Detector / category

<!-- e.g. reflection-pattern, dynamic-proxy-pattern, etc. -- see the `detectorId` field in report.json -->

## GraalVM version and native-image outcome

<!-- Did the actual native-image build succeed? Which GraalVM version? -->
