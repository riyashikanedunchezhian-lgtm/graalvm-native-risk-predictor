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

class NonConstantResourceBundleDetectorTest {

    private final NonConstantResourceBundleDetector detector = new NonConstantResourceBundleDetector();

    @Test
    void flagsNonConstantResourceBundle() {
        ClassNode classNode = classWithMethodCallingNonConstant(
                "java/util/ResourceBundle", "getBundle",
                "(Ljava/lang/Object;)Ljava/util/ResourceBundle;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertEquals(1, findings.size());
        assertEquals(FindingCategory.NON_CONSTANT_RESOURCE_LOAD, findings.get(0).getCategory());
    }

    @Test
    void doesNotFlagConstantResourceBundle() {
        ClassNode classNode = classWithMethodCallingConstant(
                "java/util/ResourceBundle", "getBundle",
                "(Ljava/lang/String;)Ljava/util/ResourceBundle;"
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

    /** Builds a minimal class with one method calling ResourceBundle.getBundle with a non-constant argument. */
    private ClassNode classWithMethodCallingNonConstant(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        // Load a non-constant argument (e.g., a method parameter or field)
        mv.visitVarInsn(ALOAD, 0); // Load 'this' as Object
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

    /** Builds a minimal class with one method calling ResourceBundle.getBundle with a constant argument. */
    private ClassNode classWithMethodCallingConstant(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn("my.resources.Bundle"); // Constant string
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