package io.nativerisk.core.recommendation;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Recommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps each Finding to a specific, actionable fix. Kept as simple
 * per-category templates for Phase 1 -- see docs/proposal.md Future
 * Work for the optional (clearly-labeled-as-a-convenience-layer) idea
 * of using an LLM to expand these into longer explanations later.
 */
public final class RecommendationEngine {

    public List<Recommendation> recommend(List<Finding> findings) {
        List<Recommendation> recommendations = new ArrayList<>();
        for (Finding finding : findings) {
            recommendations.add(new Recommendation(finding, fixFor(finding)));
        }
        return recommendations;
    }

    private String fixFor(Finding finding) {
        FindingCategory category = finding.getCategory();
        return switch (category) {
            case REFLECTION -> "Add the target class/member to reflect-config.json, "
                    + "or run the GraalVM tracing agent to auto-generate an entry.";
            case DYNAMIC_PROXY -> "Register the implemented interface(s) in proxy-config.json.";
            case JNI -> "Add the native method signature to jni-config.json.";
            case NON_CONSTANT_RESOURCE_LOAD -> "Verify the resource path is available at build time, "
                    + "add it explicitly to resource-config.json, or pass --allow-incomplete-classpath "
                    + "if the fallback is acceptable.";
            case SERIALIZATION -> "Register the serialized class(es) in reflect-config.json "
                    + "(serialization uses reflective field access) and verify with the tracing agent "
                    + "if the concrete types are only known at runtime.";
            case CUSTOM_CLASS_LOADER -> "Custom class loading is difficult to support under Native Image's "
                    + "closed-world model. Consider whether the dynamically loaded classes can be made "
                    + "statically reachable, or explicitly registered if using a supported dynamic-class mechanism.";
            case INVOKE_DYNAMIC -> "Review the bootstrap method's runtime behavior; if it dynamically "
                    + "generates or loads classes, those may need explicit registration.";
            case DEPENDENCY_KNOWN_RISKY_LIBRARY -> "Check the library's entry in the GraalVM "
                    + "reachability-metadata repository (or its own native-image support docs) for "
                    + "required configuration.";
        };
    }
}
