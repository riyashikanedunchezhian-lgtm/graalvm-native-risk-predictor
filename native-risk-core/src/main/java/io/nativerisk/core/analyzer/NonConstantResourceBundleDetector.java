package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags calls to {@code ResourceBundle.getBundle} where the baseName argument is NOT a compile-time constant string.
 * Non-constant bundle names prevent GraalVM from performing static analysis to determine which
 * resource bundles might be needed at runtime, often leading to missing bundles in the native image.
 *
 * <p>This follows the same pattern as {@link NonConstantClassForNameDetector} and {@link ResourceAccessDetector}
 * but targets ResourceBundle.getBundle specifically.</p>
 *
 * <p>Known limitation: This is a shallow, single-instruction-lookback heuristic -- it will
 * miss constants built via StringBuilder concatenation a few instructions earlier, and will
 * not flag anything if the previous instruction happens to be an LDC for an unrelated reason.
 * It trades completeness for a low false-negative-inducing implementation cost;
 * see docs/limitations.md.</p>
 */
public final class NonConstantResourceBundleDetector implements BytecodeDetector {

    private static final String ID = "non-constant-resource-bundle";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> detect(ClassNode classNode) {
        List<Finding> findings = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            AbstractInsnNode prev = null;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call &&
                        "java/util/ResourceBundle".equals(call.owner) &&
                        "getBundle".equals(call.name) &&
                        call.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC) {

                    boolean argIsConstant = prev instanceof LdcInsnNode ldc && ldc.cst instanceof String;

                    if (!argIsConstant) {
                        String location = classNode.name.replace('/', '.') + "." + method.name;
                        String description = "Non-constant ResourceBundle.getBundle call -- the bundle base name cannot be determined at build time, "
                                + "which may prevent GraalVM from including the required resource bundle in the native image. "
                                + "Consider using a constant bundle name or providing explicit resource configuration.";

                        findings.add(new Finding(FindingCategory.NON_CONSTANT_RESOURCE_LOAD, Severity.MEDIUM, location, description, ID));
                    }
                }
                prev = insn;
            }
        }
        return findings;
    }
}