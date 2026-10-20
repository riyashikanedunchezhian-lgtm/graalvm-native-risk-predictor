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

/**
 * Flags calls to {@code java.util.ServiceLoader.load} which indicate dynamic service discovery.
 * ServiceLoader usage requires a corresponding <code>META-INF/services</code> file to be present
 * and visible at native image build time, otherwise the service will not be found at runtime.
 *
 * <p>Known limitation: This is a syntactic check and cannot determine if the service is actually
 * available or if the call is dead code. It also does not flag the iterator usage pattern
 * (<code>ServiceLoader.load(...).iterator()</code>) which is equally problematic.
 * See docs/limitations.md.</p>
 */
public final class ServiceLoaderDetector implements BytecodeDetector {

    private static final String ID = "service-loader-load";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> detect(ClassNode classNode) {
        List<Finding> findings = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call &&
                        "java/util/ServiceLoader".equals(call.owner) &&
                        "load".equals(call.name) &&
                        call.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC) {

                    String location = classNode.name.replace('/', '.') + "." + method.name;
                    String description = "Call to java.util.ServiceLoader.load() -- dynamic service discovery "
                            + "requires META-INF/services files to be present at build time. "
                            + "Consider using explicit service registration or GraalVM's service-loader configuration.";

                    findings.add(new Finding(FindingCategory.SERVICE_LOADER, Severity.MEDIUM, location, description, ID));
                }
            }
        }
        return findings;
    }
}