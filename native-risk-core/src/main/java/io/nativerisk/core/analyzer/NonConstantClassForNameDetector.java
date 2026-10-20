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
 * Flags calls to {@code Class.forName} where the argument is NOT a compile-time constant string.
 * Non-constant class names prevent GraalVM from performing static analysis to determine which
 * classes might be needed at runtime, often leading to reflection-based fallbacks or missing
 * resources in the native image.
 *
 * <p>This is a narrower version of the general reflection detector that focuses specifically
 * on the class name argument being non-constant.</p>
 *
 * <p>Known limitation: This is a shallow, single-instruction-lookback heuristic -- it will
 * miss constants built via StringBuilder concatenation a few instructions earlier, and will
 * not flag anything if the previous instruction happens to be an LDC for an unrelated reason.
 * It trades completeness for a low false-negative-inducing implementation cost;
 * see docs/limitations.md.</p>
 */
public final class NonConstantClassForNameDetector implements BytecodeDetector {

    private static final String ID = "non-constant-class-forname";

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
                        "java/lang/Class".equals(call.owner) &&
                        "forName".equals(call.name) &&
                        call.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC) {

                    boolean argIsConstant = prev instanceof LdcInsnNode ldc && ldc.cst instanceof String;

                    if (!argIsConstant) {
                        String location = classNode.name.replace('/', '.') + "." + method.name;
                        String description = "Non-constant Class.forName call -- the class name cannot be determined at build time, "
                                + "which may prevent GraalVM from including the required class in the native image. "
                                + "Consider using a constant class name or providing explicit reflection configuration.";

                        findings.add(new Finding(FindingCategory.REFLECTION, Severity.HIGH, location, description, ID));
                    }
                }
                prev = insn;
            }
        }
        return findings;
    }
}