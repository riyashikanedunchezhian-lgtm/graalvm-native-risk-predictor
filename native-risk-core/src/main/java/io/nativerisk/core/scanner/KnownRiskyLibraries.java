package io.nativerisk.core.scanner;

import java.util.List;

/**
 * Seed data for the dependency scanner. Manually curated for Phase 1
 * -- see KnownRiskyLibrary for the plan to make this self-updating.
 */
public final class KnownRiskyLibraries {

    public static final List<KnownRiskyLibrary> TABLE = List.of(
            new KnownRiskyLibrary("com.fasterxml.jackson", "Jackson",
                    "Reflection-based (de)serialization; needs reflect-config.json per serialized type."),
            new KnownRiskyLibrary("org.hibernate", "Hibernate ORM",
                    "Proxy-based lazy loading and reflective entity access; needs proxy-config.json and reflect-config.json."),
            new KnownRiskyLibrary("com.google.protobuf", "Protocol Buffers",
                    "Generated classes often use reflection for descriptors; verify reachability-metadata coverage."),
            new KnownRiskyLibrary("org.apache.commons.beanutils", "Commons BeanUtils",
                    "Heavy reliance on reflective bean introspection."),
            new KnownRiskyLibrary("javax.xml.bind", "JAXB",
                    "Reflective XML binding; typically needs substantial reflect-config.json entries."),
            new KnownRiskyLibrary("org.springframework.data", "Spring Data",
                    "Repository proxies generated dynamically at runtime; verify Spring AOT coverage.")
    );

    private KnownRiskyLibraries() {
    }
}
