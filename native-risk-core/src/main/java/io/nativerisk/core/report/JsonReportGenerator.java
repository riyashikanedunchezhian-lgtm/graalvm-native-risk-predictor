package io.nativerisk.core.report;

import com.google.gson.GsonBuilder;
import io.nativerisk.core.model.CompatibilityReport;
import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.Recommendation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a CompatibilityReport as machine-readable JSON, suitable
 * for CI gating (e.g. failing a pipeline if riskLevel == "HIGH" or
 * score is below a threshold).
 */
public final class JsonReportGenerator {

    public String toJson(CompatibilityReport report) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("compatibilityScore", report.getCompatibilityScore());
        root.put("riskLevel", report.getRiskLevel().toString());
        root.put("scoringMethod", report.getScoringMethod().toString());

        List<Map<String, Object>> findingsJson = new ArrayList<>();
        for (Finding f : report.getFindings()) {
            Map<String, Object> fj = new LinkedHashMap<>();
            fj.put("category", f.getCategory().toString());
            fj.put("severity", f.getSeverity().toString());
            fj.put("location", f.getLocation());
            fj.put("description", f.getDescription());
            fj.put("detectorId", f.getDetectorId());
            findingsJson.add(fj);
        }
        root.put("findings", findingsJson);

        List<Map<String, Object>> recsJson = new ArrayList<>();
        for (Recommendation r : report.getRecommendations()) {
            Map<String, Object> rj = new LinkedHashMap<>();
            rj.put("location", r.getFinding().getLocation());
            rj.put("category", r.getFinding().getCategory().toString());
            rj.put("actionableFix", r.getActionableFix());
            recsJson.add(rj);
        }
        root.put("recommendations", recsJson);

        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    public void writeToFile(CompatibilityReport report, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, toJson(report), StandardCharsets.UTF_8);
    }
}
