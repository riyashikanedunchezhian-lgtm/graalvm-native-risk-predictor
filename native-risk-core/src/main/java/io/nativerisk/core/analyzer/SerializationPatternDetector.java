package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags Java serialization usage: construction of ObjectInputStream /
 * ObjectOutputStream, and classes that declare writeObject/readObject/
 * writeReplace/readResolve. Serialization relies on reflective field
 * access and dynamic class resolution (readObject resolves classes by
 * name from the stream), which is one of the more common and less
 * obvious native-image failure sources -- it was missing from the
 * original detector list and was added based on review feedback.
 */
public final class SerializationPatternDetector implements BytecodeDetector {

    private static final String ID = "serialization-pattern";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> detect(ClassNode classNode) {
        List<Finding> findings = new ArrayList<>();

        // 1. Declared serialization hook methods.
        for (MethodNode method : classNode.methods) {
            boolean isSerializationHook = switch (method.name) {
                case "writeObject", "readObject", "writeReplace", "readResolve", "readObjectNoData" -> true;
                default -> false;
            };
            if (isSerializationHook) {
                String location = classNode.name.replace('/', '.') + "." + method.name;
                findings.add(new Finding(
                        FindingCategory.SERIALIZATION,
                        Severity.MEDIUM,
                        location,
                        "Custom serialization hook (" + method.name + ") -- Java serialization resolves "
                                + "classes reflectively at runtime; ensure the relevant classes are registered "
                                + "for reflection if serialized/deserialized in the native image.",
                        ID));
            }
        }

        // 2. ObjectInputStream / ObjectOutputStream construction.
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof TypeInsnNode typeInsn
                        && "new".equals(opcodeName(typeInsn))
                        && (typeInsn.desc.equals("java/io/ObjectInputStream")
                        || typeInsn.desc.equals("java/io/ObjectOutputStream"))) {

                    String location = classNode.name.replace('/', '.') + "." + method.name;
                    findings.add(new Finding(
                            FindingCategory.SERIALIZATION,
                            Severity.MEDIUM,
                            location,
                            "Construction of " + typeInsn.desc.replace('/', '.') + " -- classes read/written "
                                    + "through this stream must be reflection-registered for native-image.",
                            ID));
                }
            }
        }

        return findings;
    }

    private static String opcodeName(TypeInsnNode insn) {
        // NEW opcode value is 187; kept as a named check for readability at call site.
        return insn.getOpcode() == 187 ? "new" : "other";
    }
}
