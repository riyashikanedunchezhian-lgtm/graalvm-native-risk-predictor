package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags `native` method declarations (ACC_NATIVE). JNI calls require
 * explicit jni-config.json registration and are among the least
 * automatable compatibility risks -- GraalVM cannot infer JNI
 * requirements from bytecode alone, since the native side is opaque.
 */
public final class JniPatternDetector implements BytecodeDetector {

    private static final String ID = "jni-native-method";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> detect(ClassNode classNode) {
        List<Finding> findings = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            boolean isNative = (method.access & Opcodes.ACC_NATIVE) != 0;
            if (!isNative) {
                continue;
            }

            String location = classNode.name.replace('/', '.') + "." + method.name;
            String description = "Native method declaration -- requires an entry in jni-config.json. "
                    + "GraalVM cannot infer JNI requirements from bytecode alone.";

            findings.add(new Finding(FindingCategory.JNI, Severity.HIGH, location, description, ID));
        }
        return findings;
    }
}
