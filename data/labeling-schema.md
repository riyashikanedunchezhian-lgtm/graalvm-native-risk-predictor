# Dataset Labeling Schema

Defines the exact schema for any labeled dataset used by
`benchmark/` (Phase 1 evaluation) or `native-risk-ml` (Phase 2
training), per `docs/evaluation-plan.md` and `docs/data-sourcing.md`.

## Record format (one JSON object per line, JSONL)

```json
{
  "projectRef": "github.com/org/repo@commit-sha",
  "license": "Apache-2.0",
  "graalvmVersion": "24.0.1",
  "outcome": "FALLBACK",
  "outcomeSource": "ci-log",
  "ciLogUrl": "https://github.com/org/repo/actions/runs/12345",
  "dependencyProfile": ["spring-boot", "jackson", "hibernate"],
  "labeledAt": "2026-08-01",
  "labeledBy": "mining-script-v1"
}
```

## Field notes

- `outcome` — one of `CLEAN_SUCCESS`, `FALLBACK`, `FAILURE` (see
  `docs/evaluation-plan.md` for exact definitions). No other values
  permitted; ambiguous cases are excluded rather than force-labeled.
- `outcomeSource` — must be `ci-log` (extracted directly from CI
  output). Records without a verifiable CI log are not included in
  this dataset — see `docs/data-sourcing.md` "Labeling method."
- `license` — recorded per project so downstream users can verify
  they're permitted to use the mined data; projects with incompatible
  or unclear licenses are excluded at mining time (see
  `data/scripts/mine-ci-logs.py`).
- `graalvmVersion` — required for Phase 2, since model validity is
  tied to a specific GraalVM release (see docs/data-sourcing.md
  "Retraining and staleness").

## What is NOT in this repository

No actual labeled data is checked in here. This directory holds only
the schema and mining scripts. See `.gitignore` in this directory —
any generated `.jsonl` dataset files are excluded from version control
by default. If your organization wants to check in a dataset, use
Git LFS or an external data store and document the provenance here.
