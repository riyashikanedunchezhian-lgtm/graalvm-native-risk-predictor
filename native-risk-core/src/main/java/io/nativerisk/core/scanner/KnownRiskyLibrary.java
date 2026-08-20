package io.nativerisk.core.scanner;

/**
 * A single entry in the known-risky-library table: a package or
 * annotation prefix known (from GraalVM's public reachability-metadata
 * repository, or direct experience) to typically require extra
 * native-image configuration.
 *
 * This table is intentionally small and manually seeded for Phase 1.
 * docs/limitations.md notes this needs ongoing maintenance; a future
 * improvement is to sync it periodically from
 * https://github.com/oracle/graalvm-reachability-metadata instead of
 * hand-editing this list.
 */
public record KnownRiskyLibrary(
        String matchPrefix,   // package or annotation prefix to match on the classpath
        String libraryName,   // human-readable name for reporting
        String reason
) {
}
