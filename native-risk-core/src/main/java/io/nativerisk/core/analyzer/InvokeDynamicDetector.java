package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import io.nativerisk.core.model.Severity;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags invokedynamic instructions whose bootstrap method is NOT one
 * of the JDK's own well-understood, GraalVM-supported call sites
 * (lambda metafactory, string concatenation). invokedynamic is
 * extremely common in modern Java (lambdas, method references,
 * records, string concat) and GraalVM handles the standard JDK cases
 * well -- but third-party or hand-rolled bootstrap methods (some
 * dynamic-language interop, some bytecode-generation frameworks) are
 * a real and easy-to-miss risk source. This detector was missing from
 * the original list and was added based on review feedback.
 */
public final class InvokeDynamicDetector implements BytecodeDetector {

    private static final String ID = "invokedynamic-pattern";

    // Bootstrap method owners GraalVM natively understands well; anything
    // else is flagged as a lower-confidence but worth-reviewing risk.
    private static final List<String> KNOWN_SAFE_BOOTSTRAP_OWNERS = List.of(
            "java/lang/invoke/LambdaMetafactory",
            "java/lang/invoke/StringConcatFactory"
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
                if (!(insn instanceof InvokeDynamicInsnNode indy)) {
                    continue;
                }

                String bsmOwner = indy.bsm.getOwner();
                boolean isKnownSafe = KNOWN_SAFE_BOOTSTRAP_OWNERS.contains(bsmOwner);

                if (isKnownSafe) {
                    continue; // lambdas / string concat -- well supported, not worth flagging
                }

                String location = classNode.name.replace('/', '.') + "." + method.name;
                String description = "invokedynamic call site with non-standard bootstrap method "
                        + bsmOwner.replace('/', '.') + " -- dynamic call-site linkage outside the "
                        + "well-supported lambda/string-concat cases can be a native-image risk "
                        + "and is worth manual review.";

                findings.add(new Finding(FindingCategory.INVOKE_DYNAMIC, Severity.LOW, location, description, ID));
            }
        }
        return findings;
    }
}
