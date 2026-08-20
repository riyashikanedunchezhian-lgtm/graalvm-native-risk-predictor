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
 * Flags java.lang.reflect.Proxy.newProxyInstance calls, and common
 * framework proxy-creation entry points (e.g. Spring's
 * ProxyFactory#getProxy family) where statically identifiable. These
 * require the relevant interfaces to be registered in
 * proxy-config.json for GraalVM Native Image.
 */
public final class ProxyPatternDetector implements BytecodeDetector {

    private static final String ID = "dynamic-proxy-pattern";

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

                boolean isJdkProxy = "java/lang/reflect/Proxy".equals(call.owner)
                        && "newProxyInstance".equals(call.name);

                boolean isSpringProxyFactory = call.owner.startsWith("org/springframework/aop/framework/ProxyFactory")
                        && ("getProxy".equals(call.name) || "getObject".equals(call.name));

                if (!isJdkProxy && !isSpringProxyFactory) {
                    continue;
                }

                String location = classNode.name.replace('/', '.') + "." + method.name;
                String description = isJdkProxy
                        ? "java.lang.reflect.Proxy.newProxyInstance -- register the implemented interfaces in proxy-config.json."
                        : "Spring ProxyFactory usage -- Spring AOT normally handles this, but verify proxy-config.json coverage if bypassing Spring AOT.";

                findings.add(new Finding(FindingCategory.DYNAMIC_PROXY, Severity.MEDIUM, location, description, ID));
            }
        }
        return findings;
    }
}
