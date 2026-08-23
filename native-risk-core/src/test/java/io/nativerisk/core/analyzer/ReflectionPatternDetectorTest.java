package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.ClassReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.*;

class ReflectionPatternDetectorTest {

    private final ReflectionPatternDetector detector = new ReflectionPatternDetector();

    @Test
    void flagsClassForName() {
        ClassNode classNode = classWithMethodCalling(
                "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertEquals(1, findings.size());
        assertEquals(FindingCategory.REFLECTION, findings.get(0).getCategory());
    }

    @Test
    void doesNotFlagUnrelatedCalls() {
        ClassNode classNode = classWithMethodCalling(
                "java/lang/String", "toUpperCase",
                "()Ljava/lang/String;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertTrue(findings.isEmpty());
    }

    /** Builds a minimal class with one method that calls the given owner/name/desc. */
    private ClassNode classWithMethodCalling(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn("com.example.Target");
        mv.visitMethodInsn(INVOKESTATIC, owner, name, desc, false);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();

        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        return classNode;
    }
}
