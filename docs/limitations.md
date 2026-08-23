# Known Limitations

Stated plainly, so the tool is not mistaken for something it isn't.

## Inherited static-analysis ceiling

GraalVM's own closed-world limitation exists precisely because some
dynamic behavior (e.g., `Class.forName` called with a value built from
user input or external configuration) cannot be resolved without
running the program. This tool inherits the same blind spot: it can
flag the *presence* of a dynamic call, but generally cannot determine
what class it will resolve to at runtime, or whether the flagged code
path is even reachable in practice.

## False positives and false negatives are expected

The tool is deliberately conservative — it favors false positives
(flagging things that turn out to be fine) over false negatives,
because a missed issue costs a developer a full native-image build
cycle to discover, while a false positive costs a few minutes of
review. See `evaluation-plan.md` for how this tradeoff is measured.

## No substitute for the tracing agent

GraalVM's own tracing agent remains the most reliable source of ground
truth, because it observes actual execution rather than statically
inferring reachability. This tool is positioned as an earlier, cheaper,
complementary check — not a replacement for the tracing agent, and not
a replacement for actually running a native build in CI before release.

## Library coverage is incomplete

The dependency scanner's knowledge of "known risky libraries" is only
as good as the metadata it's seeded with (currently sourced manually
from GraalVM's public reachability-metadata repository), and needs
ongoing maintenance as new library versions change their reflective
behavior.

## Detector coverage is not exhaustive

The current bytecode detectors cover: `Class.forName`,
`java.lang.reflect.*`, `java.lang.reflect.Proxy` construction, JNI
method declarations, non-constant `getResourceAsStream` arguments,
Java serialization (`ObjectInputStream`, `writeReplace`/`writeObject`),
custom `ClassLoader` definitions, and `invokedynamic`/`MethodHandle`
usage. This list is not exhaustive and will need to grow as new
failure patterns are reported (see the false-negative issue template).
