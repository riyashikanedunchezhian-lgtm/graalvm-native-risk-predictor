package io.nativerisk.core.report;

import io.nativerisk.core.model.CompatibilityReport;
import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.Recommendation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Renders a CompatibilityReport as a single, dependency-free HTML
 * file for local developer viewing. Deliberately simple (no JS
 * framework, no external assets) so the report works offline and
 * survives being emailed around or attached to a CI artifact.
 */
public final class HtmlReportGenerator {

    public String toHtml(CompatibilityReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
          .append("<title>Native Image Compatibility Report</title>")
          .append("<style>")
          .append("body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:900px;margin:2rem auto;padding:0 1rem;color:#1a1a1a;}")
          .append("h1{font-size:1.4rem;} .score{font-size:2.5rem;font-weight:700;}")
          .append(".risk-LOW{color:#1a7f37;} .risk-MEDIUM{color:#9a6700;} .risk-HIGH{color:#cf222e;}")
          .append("table{border-collapse:collapse;width:100%;margin-top:1rem;}")
          .append("th,td{border:1px solid #d0d7de;padding:.5rem;text-align:left;font-size:.9rem;vertical-align:top;}")
          .append("th{background:#f6f8fa;} .sev-HIGH{color:#cf222e;font-weight:600;} .sev-MEDIUM{color:#9a6700;} .sev-LOW{color:#57606a;}")
          .append("code{background:#f6f8fa;padding:.1rem .3rem;border-radius:4px;}")
          .append("</style></head><body>");

        sb.append("<h1>GraalVM Native Image Compatibility Report</h1>");
        sb.append("<p class=\"score risk-").append(report.getRiskLevel()).append("\">")
          .append(report.getCompatibilityScore()).append("/100</p>");
        sb.append("<p>Risk: <strong class=\"risk-").append(report.getRiskLevel()).append("\">")
          .append(report.getRiskLevel()).append("</strong> &mdash; scored by ")
          .append(report.getScoringMethod()).append(" engine</p>");

        sb.append("<h2>Detected Issues</h2><table><tr><th>Severity</th><th>Category</th><th>Location</th><th>Description</th></tr>");
        for (Finding f : report.getFindings()) {
            sb.append("<tr>")
              .append("<td class=\"sev-").append(f.getSeverity()).append("\">").append(f.getSeverity()).append("</td>")
              .append("<td>").append(f.getCategory()).append("</td>")
              .append("<td><code>").append(escape(f.getLocation())).append("</code></td>")
              .append("<td>").append(escape(f.getDescription())).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");

        sb.append("<h2>Recommendations</h2><table><tr><th>Location</th><th>Fix</th></tr>");
        List<Recommendation> recs = report.getRecommendations();
        for (Recommendation r : recs) {
            sb.append("<tr>")
              .append("<td><code>").append(escape(r.getFinding().getLocation())).append("</code></td>")
              .append("<td>").append(escape(r.getActionableFix())).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public void writeToFile(CompatibilityReport report, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, toHtml(report), StandardCharsets.UTF_8);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
