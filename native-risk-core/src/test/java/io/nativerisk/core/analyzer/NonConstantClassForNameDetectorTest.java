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

class NonConstantClassForNameDetectorTest {

    private final NonConstantClassForNameDetector detector = new NonConstantClassForNameDetector();

    @Test
    void flagsNonConstantClassForName() {
        ClassNode classNode = classWithMethodCallingNonConstant(
                "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertEquals(1, findings.size());
        assertEquals(FindingCategory.REFLECTION, findings.get(0).getCategory());
    }

    @Test
    void doesNotFlagConstantClassForName() {
        ClassNode classNode = classWithMethodCallingConstant(
                "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertTrue(findings.isEmpty());
    }

    @Test
    void doesNotFlagUnrelatedCalls() {
        ClassNode classNode = classWithMethodCalling(
                "java/lang/String", "length",
                "()I"
        );

        List<Finding> findings = detector.detect(classNode);

        assertTrue(findings.isEmpty());
    }

    /** Builds a minimal class with one method calling Class.forName with a non-constant argument. */
    private ClassNode classWithMethodCallingNonConstant(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        // Load a non-constant argument (e.g., a method parameter or field)
        mv.visitVarInsn(ALOAD, 0); // Load 'this' as Object
        // Use INVOKESTATIC (the detector looks for INVOKESTATIC) to match the expected opcode.
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

    /** Builds a minimal class with one method calling Class.forName with a constant argument. */
    private ClassNode classWithMethodCallingConstant(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn("java.lang.String"); // Constant string
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

    /** Builds a minimal class with one method calling the given method. */
    private ClassNode classWithMethodCalling(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
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