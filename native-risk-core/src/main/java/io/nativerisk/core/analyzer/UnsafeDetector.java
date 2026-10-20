package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags usage of sun.misc.Unsafe, which is prohibited in GraalVM Native Image
 * as it breaks JVM safety guarantees and prevents static analysis.
 *
 * <p>This detector flags:
 * <ul>
 *   <li>Field accesses to {@code sun.misc.Unsafe.theUnsafe}</li>
 *   <li>Instantiation via {@code sun.misc.Unsafe#getInstance()}</li>
 *   <li>Any method calls on {@code sun.misc.Unsafe} instances</li>
 * </ul>
 *
 * <p>Known limitation: This is a syntactic check and cannot determine if
 * the Unsafe instance is actually used in a dangerous way, or if the code
 * is dead/reachable only via reflection. See docs/limitations.md.
 */
public final class UnsafeDetector implements BytecodeDetector {

    private static final String ID = "unsafe-usage";

    private static final String UNSAFE_CLASS = "sun/misc/Unsafe";
    private static final String THE_UNSAFE_FIELD = "theUnsafe";
    private static final String GET_INSTANCE_METHOD = "getInstance";

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
                // Check for static field access: sun.misc.Unsafe.theUnsafe
                if (insn instanceof FieldInsnNode field &&
                        field.owner.equals(UNSAFE_CLASS) &&
                        field.name.equals(THE_UNSAFE_FIELD) &&
                        (field.getOpcode() == org.objectweb.asm.Opcodes.GETSTATIC ||
                         field.getOpcode() == org.objectweb.asm.Opcodes.PUTSTATIC)) {

                    String location = classNode.name.replace('/', '.') + "." + method.name;
                    String description = "Access to sun.misc.Unsafe.theUnsafe -- prohibited in GraalVM Native Image. "
                            + "Consider using safer alternatives like java.lang.invoke.VarHandle";

                    findings.add(new Finding(FindingCategory.UNSAFE, Severity.HIGH, location, description, ID));
                }

                // Check for static method call: sun.misc.Unsafe#getInstance()
                if (insn instanceof MethodInsnNode call &&
                        call.owner.equals(UNSAFE_CLASS) &&
                        call.name.equals(GET_INSTANCE_METHOD) &&
                        call.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC) {

                    String location = classNode.name.replace('/', '.') + "." + method.name;
                    String description = "Call to sun.misc.Unsafe#getInstance() -- prohibited in GraalVM Native Image. "
                            + "Consider using safer alternatives like java.lang.invoke.VarHandle";

                    findings.add(new Finding(FindingCategory.UNSAFE, Severity.HIGH, location, description, ID));
                }

                // Check for instance method calls on Unsafe (after getting instance)
                // Skip if the previous instruction was the static field access for theUnsafe –
                // otherwise we would report two findings (field + instance call) for the same unsafe usage.
                if (insn instanceof MethodInsnNode call &&
                        call.owner.equals(UNSAFE_CLASS) &&
                        call.getOpcode() != org.objectweb.asm.Opcodes.INVOKESTATIC) { // Not static call
                    // Determine if the previous instruction was the field access to theUnsafe
                    boolean prevWasUnsafeField = false;
                    if (prev instanceof org.objectweb.asm.tree.FieldInsnNode f) {
                        prevWasUnsafeField = f.owner.equals(UNSAFE_CLASS) && f.name.equals(THE_UNSAFE_FIELD);
                    }
                    if (!prevWasUnsafeField) {
                        String location = classNode.name.replace('/', '.') + "." + method.name;
                        String description = "Call to sun.misc.Unsafe method (" + call.name + ") -- prohibited in GraalVM Native Image. "
                                + "Consider using safer alternatives like java.lang.invoke.VarHandle";
                        findings.add(new Finding(FindingCategory.UNSAFE, Severity.HIGH, location, description, ID));
                    }
                }
                prev = insn;
            }
        }
        return findings;
    }
}