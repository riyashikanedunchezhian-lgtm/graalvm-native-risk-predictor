package io.nativerisk.core.scanner;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans the project's runtime classpath with ClassGraph for packages
 * matching entries in KnownRiskyLibraries.TABLE. This does NOT
 * initialize or load any classes (ClassGraph reads bytecode/metadata
 * only), matching the "no side effects" requirement for a pre-build
 * check.
 */
public final class DependencyRiskScanner {

    private final List<KnownRiskyLibrary> table;

    public DependencyRiskScanner() {
        this(KnownRiskyLibraries.TABLE);
    }

    public DependencyRiskScanner(List<KnownRiskyLibrary> table) {
        this.table = table;
    }

    /**
     * @param classpathEntries directories/jars to scan (typically the project's runtime classpath)
     */
    public List<Finding> scan(List<String> classpathEntries) {
        List<Finding> findings = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath((Object[]) classpathEntries.toArray(new String[0]))
                .enableClassInfo()
                .enableAnnotationInfo()
                .scan()) {

            for (KnownRiskyLibrary lib : table) {
                ClassInfoList matches = scanResult.getAllClasses()
                        .filter(ci -> ci.getName().startsWith(lib.matchPrefix()));

                if (!matches.isEmpty()) {
                    findings.add(new Finding(
                            FindingCategory.DEPENDENCY_KNOWN_RISKY_LIBRARY,
                            Severity.MEDIUM,
                            lib.libraryName(),
                            matches.size() + " class(es) under " + lib.matchPrefix()
                                    + " detected on classpath. " + lib.reason(),
                            "dependency-scanner:" + lib.matchPrefix()
                    ));
                }
            }
        }

        return findings;
    }
}
