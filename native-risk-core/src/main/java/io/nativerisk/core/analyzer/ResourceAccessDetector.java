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
import java.util.Set;

/**
 * Flags getResourceAsStream / getResource calls whose argument is NOT
 * a compile-time constant string (i.e., the instruction immediately
 * preceding the call is not an LDC of a String). Resources not on the
 * classpath at build time need explicit resource-config.json entries.
 *
 * This is a shallow, single-instruction-lookback heuristic -- it will
 * miss constants built via StringBuilder concatenation a few
 * instructions earlier, and will not flag anything if the previous
 * instruction happens to be an LDC for an unrelated reason. It trades
 * completeness for a low false-negative-inducing implementation cost;
 * see docs/limitations.md.
 */
public final class ResourceAccessDetector implements BytecodeDetector {

    private static final String ID = "non-constant-resource-load";

    private static final Set<String> RESOURCE_METHODS = Set.of(
            "getResourceAsStream", "getResource", "getResources"
    );

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
                if (insn instanceof MethodInsnNode call && RESOURCE_METHODS.contains(call.name)
                        && (call.owner.equals("java/lang/Class") || call.owner.equals("java/lang/ClassLoader"))) {

                    boolean argIsConstant = prev instanceof LdcInsnNode ldc && ldc.cst instanceof String;

                    if (!argIsConstant) {
                        String location = classNode.name.replace('/', '.') + "." + method.name;
                        String description = "Resource lookup (" + call.name + ") with a non-constant "
                                + "(or non-trivially-constant) argument -- verify the resource is available "
                                + "at build time, or add it explicitly to resource-config.json.";
                        findings.add(new Finding(FindingCategory.NON_CONSTANT_RESOURCE_LOAD, Severity.LOW, location, description, ID));
                    }
                }
                prev = insn;
            }
        }
        return findings;
    }
}
