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

class UnsafeDetectorTest {

    private final UnsafeDetector detector = new UnsafeDetector();

    @Test
    void flagsUnsafeFieldAccess() {
        ClassNode classNode = classWithMethodAccessingField(
                "sun/misc/Unsafe", "theUnsafe",
                "Lsun/misc/Unsafe;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertEquals(1, findings.size());
        assertEquals(FindingCategory.UNSAFE, findings.get(0).getCategory());
    }

    @Test
    void flagsUnsafeGetInstanceCall() {
        ClassNode classNode = classWithMethodCalling(
                "sun/misc/Unsafe", "getInstance",
                "()Lsun/misc/Unsafe;"
        );

        List<Finding> findings = detector.detect(classNode);

        assertEquals(1, findings.size());
        assertEquals(FindingCategory.UNSAFE, findings.get(0).getCategory());
    }

    @Test
    void flagsUnsafeInstanceMethodCall() {
        ClassNode classNode = classWithUnsafeInstanceMethodCall();
        // The detector will flag either the field access or the instance method call (but not both).
        // We only need to assert that at least one unsafe usage is reported.
        List<Finding> findings = detector.detect(classNode);
        assertEquals(1, findings.size());
        assertEquals(FindingCategory.UNSAFE, findings.get(0).getCategory());
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

    /** Builds a minimal class with one method accessing the given field. */
    private ClassNode classWithMethodAccessingField(String owner, String name, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, owner, name, desc);
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

    /** Builds a minimal class with one method calling an instance method on an Unsafe reference. */
    private ClassNode classWithUnsafeInstanceMethodCall() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/Sample", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        // Simulate a null Unsafe reference (or any reference); the detector flags any instance method call on Unsafe.
        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "allocateMemory", "(J)J", false);
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