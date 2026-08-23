package io.nativerisk.core.analyzer;

import io.nativerisk.core.model.Finding;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

/**
 * Contract for a single ASM-based pattern detector. Each detector is
 * responsible for one narrow category of risky bytecode pattern (see
 * FindingCategory) and should be independently testable against a
 * single compiled class.
 *
 * Implementations should NOT throw on unexpected bytecode shapes --
 * prefer skipping ambiguous cases over crashing the whole analysis
 * run for one class.
 */
public interface BytecodeDetector {

    /**
     * Stable identifier used in Finding#getDetectorId() and in
     * ScoringWeights lookups. Should not change across releases once
     * published, since it may be referenced in suppression config.
     */
    String id();

    /**
     * Inspect a single class and return zero or more Findings.
     *
     * @param classNode the parsed class (already visited by ClassReader with EXPAND_FRAMES)
     * @return findings for this class; empty list if nothing detected
     */
    List<Finding> detect(ClassNode classNode);
}
