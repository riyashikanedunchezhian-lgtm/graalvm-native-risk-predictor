package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import io.nativerisk.core.model.FindingCategory;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.*;

class JniPatternDetectorTest {

    private final JniPatternDetector detector = new JniPatternDetector();

    @Test
    void flagsNativeMethod() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/NativeThing", null, "java/lang/Object", null);
        cw.visitMethod(ACC_PUBLIC | ACC_NATIVE, "doNativeWork", "()V", null, null).visitEnd();
        cw.visitEnd();

        ClassNode classNode = parse(cw);
        List<Finding> findings = detector.detect(classNode);

        assertEquals(1, findings.size());
        assertEquals(FindingCategory.JNI, findings.get(0).getCategory());
    }

    @Test
    void doesNotFlagRegularMethod() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC, "com/example/RegularThing", null, "java/lang/Object", null);
        var mv = cw.visitMethod(ACC_PUBLIC, "doRegularWork", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();

        ClassNode classNode = parse(cw);
        List<Finding> findings = detector.detect(classNode);

        assertTrue(findings.isEmpty());
    }

    private ClassNode parse(ClassWriter cw) {
        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        return classNode;
    }
}
