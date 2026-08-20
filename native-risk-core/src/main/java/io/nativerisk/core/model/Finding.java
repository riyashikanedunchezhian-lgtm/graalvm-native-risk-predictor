package io.nativerisk.core.model;

import java.util.Objects;

/**
 * A single detected compatibility risk: what was found, where, how
 * severe it looks, and why it was flagged. Findings are the common
 * currency between the bytecode analyzer, the dependency scanner, the
 * scoring engine, and the recommendation engine.
 */
public final class Finding {

    private final FindingCategory category;
    private final Severity severity;
    private final String location;      // e.g. fully-qualified class + method
    private final String description;   // human-readable explanation
    private final String detectorId;    // which detector/rule produced this

    public Finding(FindingCategory category,
                    Severity severity,
                    String location,
                    String description,
                    String detectorId) {
        this.category = Objects.requireNonNull(category, "category");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.location = Objects.requireNonNull(location, "location");
        this.description = Objects.requireNonNull(description, "description");
        this.detectorId = Objects.requireNonNull(detectorId, "detectorId");
    }

    public FindingCategory getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getDetectorId() {
        return detectorId;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + category + " in " + location + " -- " + description;
    }
}
