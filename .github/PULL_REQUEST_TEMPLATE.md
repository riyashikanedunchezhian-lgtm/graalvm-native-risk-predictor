## What this changes

## Why

## Testing

- [ ] `./gradlew test` passes
- [ ] `./gradlew :native-risk-gradle-plugin:functionalTest` passes (if touching the Gradle plugin)
- [ ] New/changed detector has both a positive test (pattern flagged) and a negative test (similar-but-safe code not flagged)

## If this changes ScoringWeights

- [ ] The reason for the change is explained in the commit message or here (see ScoringWeights.java javadoc — weights should stay auditable, not just tuned by feel)
