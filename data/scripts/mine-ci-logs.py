#!/usr/bin/env python3
"""
Skeleton for the candidate-source mining step described in
docs/data-sourcing.md. NOT a working, ready-to-run scraper -- it
documents the intended flow and the checks that matter (license
filtering, direct log-based labeling, exclusion of ambiguous cases)
so a real implementation can be built against a specific data source
(e.g. the GitHub Search/Actions API) without re-deriving the policy
decisions from scratch.

Usage (once implemented):
    python mine-ci-logs.py --query "native-image gradle" --out ../raw/candidates.jsonl

Policy this script MUST enforce (see docs/data-sourcing.md):
  1. Only include repositories whose license permits this kind of
     mining/analysis. Do not assume permissive just because a repo
     is public.
  2. Only label from CI logs directly (build success/fallback/failure
     text), never inferred from indirect signals like "PR merged" or
     "no issues filed".
  3. If a project's CI logs are unavailable or ambiguous, EXCLUDE the
     project. Do not guess a label to hit a dataset size target.
  4. Record license, CI log URL, and GraalVM version alongside each
     label, per labeling-schema.md -- a label without provenance is
     not usable.
"""

import argparse
import json
import sys


ALLOWED_LICENSES = {
    "Apache-2.0", "MIT", "BSD-3-Clause", "BSD-2-Clause", "EPL-2.0",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--query", required=True, help="Search query for candidate repos")
    parser.add_argument("--out", required=True, help="Output JSONL path")
    args = parser.parse_args()

    print(
        "This is a policy skeleton, not a working scraper. "
        "Implement search_candidate_repos(), extract_ci_outcome(), and "
        "check_license() below against a real data source (e.g. the "
        "GitHub REST/GraphQL API) before running this for real.",
        file=sys.stderr,
    )
    sys.exit(1)


def search_candidate_repos(query: str):
    """Find repos using the GraalVM native-image Gradle/Maven plugin. NOT IMPLEMENTED."""
    raise NotImplementedError


def check_license(repo_license: str) -> bool:
    """Only mine repos under a license in ALLOWED_LICENSES."""
    return repo_license in ALLOWED_LICENSES


def extract_ci_outcome(ci_log_text: str):
    """
    Parse a CI log for a native-image build outcome. Must return one of
    CLEAN_SUCCESS / FALLBACK / FAILURE, or None if ambiguous (caller
    must then EXCLUDE the record -- see module docstring policy #3).
    NOT IMPLEMENTED.
    """
    raise NotImplementedError


if __name__ == "__main__":
    main()
