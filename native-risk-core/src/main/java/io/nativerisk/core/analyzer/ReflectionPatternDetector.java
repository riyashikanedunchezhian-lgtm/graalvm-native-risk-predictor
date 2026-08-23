package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Flags calls to Class.forName and the core java.lang.reflect APIs
 * (Method.invoke, Field.get/set, Constructor.newInstance, etc).
 * These require explicit reflect-config.json entries under GraalVM
 * Native Image unless the target is otherwise statically resolvable.
 *
 * Known limitation: this is a purely syntactic check on method-call
 * targets. It cannot determine whether the resolved Class is actually
 * reachable, or whether the call site is dead code -- see
 * docs/limitations.md.
 */
public final class ReflectionPatternDetector implements BytecodeDetector {

    private static final String ID = "reflection-pattern";

    private static final Set<String> REFLECT_OWNERS = Set.of(
            "java/lang/Class",
            "java/lang/reflect/Method",
            "java/lang/reflect/Field",
            "java/lang/reflect/Constructor",
            "java/lang/reflect/Array"
    );

    private static final Set<String> INTERESTING_CLASS_METHODS = Set.of(
            "forName", "newInstance", "getMethod", "getDeclaredMethod",
            "getField", "getDeclaredField", "getConstructor", "getDeclaredConstructor",
            "getMethods", "getDeclaredMethods", "getFields", "getDeclaredFields"
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> detect(ClassNode classNode) {
        List<Finding> findings = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) {
                    continue;
                }

                boolean isReflectOwner = REFLECT_OWNERS.contains(call.owner);
                boolean isInterestingClassCall = "java/lang/Class".equals(call.owner)
                        && INTERESTING_CLASS_METHODS.contains(call.name);

                if (!isReflectOwner) {
                    continue;
                }

                Severity severity = "java/lang/Class".equals(call.owner) && "forName".equals(call.name)
                        ? Severity.HIGH
                        : Severity.MEDIUM;

                String location = classNode.name.replace('/', '.') + "." + method.name;
                String description = "Reflective call to " + call.owner.replace('/', '.')
                        + "#" + call.name + " -- requires an entry in reflect-config.json"
                        + " unless GraalVM's build-time analysis resolves it automatically.";

                findings.add(new Finding(FindingCategory.REFLECTION, severity, location, description, ID));

                // Avoid double-counting: only note the interesting-class-call reasoning once.
                if (isInterestingClassCall) {
                    // no-op branch kept for readability of intent; severity already set above
                }
            }
        }
        return findings;
    }
}
