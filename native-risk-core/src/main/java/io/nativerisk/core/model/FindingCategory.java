package io.nativerisk.core.model;

/**
 * The broad kind of compatibility risk a Finding represents. Each
 * category corresponds to one BytecodeDetector or scanner rule, and
 * has its own base weight in ScoringWeights.
 */
public enum FindingCategory {
    REFLECTION,
    DYNAMIC_PROXY,
    JNI,
    NON_CONSTANT_RESOURCE_LOAD,
    SERIALIZATION,
    CUSTOM_CLASS_LOADER,
    INVOKE_DYNAMIC,
    DEPENDENCY_KNOWN_RISKY_LIBRARY
}
