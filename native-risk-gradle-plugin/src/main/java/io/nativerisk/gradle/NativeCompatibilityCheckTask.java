package io.nativerisk.gradle;

import io.nativerisk.core.model.CompatibilityReport;
import io.nativerisk.core.pipeline.AnalysisPipeline;
import io.nativerisk.core.report.HtmlReportGenerator;
import io.nativerisk.core.report.JsonReportGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The `nativeCompatibilityCheck` Gradle task. Thin wrapper around
 * AnalysisPipeline (native-risk-core) -- all analysis logic lives
 * there so it stays reusable outside Gradle.
 *
 * This task deliberately does NOT fail the build by default; it's
 * meant as an informational pre-build check. CI pipelines that want
 * to gate on risk level should inspect report.json, e.g. via a
 * separate script/task that fails on riskLevel == "HIGH".
 */
public abstract class NativeCompatibilityCheckTask extends DefaultTask {

    @InputFiles
    public abstract DirectoryProperty getCompiledClassesDir();

    @Classpath
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getReportDirectory();

    @TaskAction
    public void run() throws IOException {
        Path compiledClassesDir = getCompiledClassesDir().get().getAsFile().toPath();
        List<String> classpathEntries = getRuntimeClasspath().getFiles().stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());

        AnalysisPipeline pipeline = new AnalysisPipeline();
        CompatibilityReport report = pipeline.analyze(compiledClassesDir, classpathEntries);

        Path reportDir = getReportDirectory().get().getAsFile().toPath();

        new HtmlReportGenerator().writeToFile(report, reportDir.resolve("report.html"));
        new JsonReportGenerator().writeToFile(report, reportDir.resolve("report.json"));

        printSummary(report);
    }

    private void printSummary(CompatibilityReport report) {
        getLogger().lifecycle("");
        getLogger().lifecycle("Compatibility Score: {}/100   Risk: {}   ({} engine)",
                report.getCompatibilityScore(), report.getRiskLevel(), report.getScoringMethod());
        getLogger().lifecycle("Findings: {}", report.getFindings().size());
        report.getFindings().forEach(f ->
                getLogger().lifecycle("  [{}] {} -- {}", f.getSeverity(), f.getLocation(), f.getDescription()));
        getLogger().lifecycle("");
        getLogger().lifecycle("Full report: build/reports/native-risk/report.html");
    }
}
