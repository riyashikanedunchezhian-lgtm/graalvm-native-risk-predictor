package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags classes that extend java.lang.ClassLoader (custom class
 * loaders). Custom class loading is fundamentally at odds with
 * Native Image's closed-world, build-time class resolution model --
 * classes loaded via a custom loader at runtime generally cannot be
 * discovered by static analysis at all. This was missing from the
 * original detector list and was added based on review feedback.
 */
public final class ClassLoaderPatternDetector implements BytecodeDetector {

    private static final String ID = "custom-classloader-pattern";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> detect(ClassNode classNode) {
        List<Finding> findings = new ArrayList<>();

        if (classNode.superName != null && classNode.superName.contains("ClassLoader")) {
            String location = classNode.name.replace('/', '.');
            findings.add(new Finding(
                    FindingCategory.CUSTOM_CLASS_LOADER,
                    Severity.HIGH,
                    location,
                    "Custom ClassLoader subclass (" + classNode.superName.replace('/', '.') + "). "
                            + "Native Image's closed-world analysis generally cannot discover classes "
                            + "loaded dynamically through a custom loader -- this is one of the harder "
                            + "patterns to fully mitigate and may require redesign rather than config.",
                    ID));
        }
        return findings;
    }
}
