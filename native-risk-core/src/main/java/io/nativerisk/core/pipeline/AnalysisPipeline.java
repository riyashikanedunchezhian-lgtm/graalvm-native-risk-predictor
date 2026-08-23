package io.nativerisk.core.pipeline;

import io.nativerisk.core.analyzer.BytecodeDetector;
import io.nativerisk.core.analyzer.ClassLoaderPatternDetector;
import io.nativerisk.core.analyzer.InvokeDynamicDetector;
import io.nativerisk.core.analyzer.JniPatternDetector;
import io.nativerisk.core.analyzer.ProxyPatternDetector;
import io.nativerisk.core.analyzer.ReflectionPatternDetector;
import io.nativerisk.core.analyzer.ResourceAccessDetector;
import io.nativerisk.core.analyzer.SerializationPatternDetector;
import io.nativerisk.core.model.CompatibilityReport;
import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.Recommendation;
import io.nativerisk.core.model.RiskLevel;
import io.nativerisk.core.recommendation.RecommendationEngine;
import io.nativerisk.core.scanner.DependencyRiskScanner;
import io.nativerisk.core.scoring.HeuristicScoringEngine;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orchestrates the full analysis pipeline described in
 * docs/architecture.md, steps 2-6 (bytecode analysis through
 * recommendation generation). Reporting (step 7) is left to the
 * caller, since the Gradle plugin and any future CLI/Maven plugin
 * will want to write reports to different locations.
 *
 * This class deliberately has no Gradle dependency, so it can be
 * reused by any build-tool integration.
 */
public final class AnalysisPipeline {

    private final List<BytecodeDetector> detectors;
    private final DependencyRiskScanner dependencyRiskScanner;
    private final HeuristicScoringEngine scoringEngine;
    private final RecommendationEngine recommendationEngine;

    public AnalysisPipeline() {
        this(defaultDetectors(), new DependencyRiskScanner(), new HeuristicScoringEngine(), new RecommendationEngine());
    }

    public AnalysisPipeline(List<BytecodeDetector> detectors,
                             DependencyRiskScanner dependencyRiskScanner,
                             HeuristicScoringEngine scoringEngine,
                             RecommendationEngine recommendationEngine) {
        this.detectors = detectors;
        this.dependencyRiskScanner = dependencyRiskScanner;
        this.scoringEngine = scoringEngine;
        this.recommendationEngine = recommendationEngine;
    }

    public static List<BytecodeDetector> defaultDetectors() {
        return List.of(
                new ReflectionPatternDetector(),
                new ProxyPatternDetector(),
                new JniPatternDetector(),
                new ResourceAccessDetector(),
                new SerializationPatternDetector(),
                new ClassLoaderPatternDetector(),
                new InvokeDynamicDetector()
        );
    }

    /**
     * Runs the full pipeline.
     *
     * @param compiledClassesDir directory containing compiled .class files (post-compileJava)
     * @param runtimeClasspath   full runtime classpath entries, for the dependency scanner
     */
    public CompatibilityReport analyze(Path compiledClassesDir, List<String> runtimeClasspath) throws IOException {
        List<Finding> findings = new ArrayList<>();
        findings.addAll(runBytecodeAnalysis(compiledClassesDir));
        findings.addAll(dependencyRiskScanner.scan(runtimeClasspath));

        int score = scoringEngine.score(findings);
        RiskLevel riskLevel = scoringEngine.riskLevelFor(score);
        List<Recommendation> recommendations = recommendationEngine.recommend(findings);

        return new CompatibilityReport(
                score,
                riskLevel,
                CompatibilityReport.ScoringMethod.HEURISTIC,
                findings,
                recommendations
        );
    }

    private List<Finding> runBytecodeAnalysis(Path compiledClassesDir) throws IOException {
        List<Finding> findings = new ArrayList<>();

        if (!Files.isDirectory(compiledClassesDir)) {
            return findings;
        }

        try (Stream<Path> paths = Files.walk(compiledClassesDir)) {
            List<Path> classFiles = paths
                    .filter(p -> p.toString().endsWith(".class"))
                    .toList();

            for (Path classFile : classFiles) {
                ClassNode classNode = parse(classFile);
                if (classNode == null) {
                    continue; // unreadable/malformed class file -- skip rather than fail the whole run
                }
                for (BytecodeDetector detector : detectors) {
                    findings.addAll(detector.detect(classNode));
                }
            }
        }

        return findings;
    }

    private ClassNode parse(Path classFile) {
        try (InputStream in = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_DEBUG);
            return classNode;
        } catch (IOException | IllegalArgumentException e) {
            // Malformed or unsupported class file version -- skip, don't crash the run.
            return null;
        }
    }
}
