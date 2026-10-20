plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    implementation(project(":native-risk-core"))
    testImplementation(libs.junit.jupiter)
}

// IMPORTANT: this sourceSets block must come BEFORE the gradlePlugin block
// below, because gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])
// needs the "functionalTest" source set to already exist. Gradle Kotlin DSL
// evaluates top-level blocks in order, so reordering these two blocks will
// reintroduce a "functionalTest source set not found" style failure.
sourceSets {
    create("functionalTest") {
        java.srcDir("src/functionalTest/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

gradlePlugin {
    plugins {
        create("nativeCompatibilityPlugin") {
            id = "io.nativerisk.native-compatibility"
            implementationClass = "io.nativerisk.gradle.NativeCompatibilityPlugin"
            displayName = "GraalVM Native Image Compatibility Checker"
            description = "Analyzes compiled bytecode and dependencies for GraalVM Native Image compatibility risk before you run native-image."
        }
    }
    // Tells the java-gradle-plugin plugin to generate plugin-under-test
    // metadata for our custom "functionalTest" source set (by default it
    // only does this for the standard "test" source set). Without this,
    // GradleRunner.withPluginClasspath() in the functional test throws
    // InvalidPluginMetadataException.
    testSourceSets.add(sourceSets["functionalTest"])
}

val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    dependsOn(functionalTest)
}

// Publishing configuration for sharing the plugin via Maven Local (or remote)
publishing {
    publications {
        create<MavenPublication>("plugin") {
            from(components["java"])
            // Optionally, you can set the artifactId, group, version if not already set by the plugin
            // group = "io.nativerisk"
            // artifactId = "native-risk-gradle-plugin"
            // version = "0.1.0-SNAPSHOT"
        }
    }
    // To publish to local Maven repository, run: ./gradlew publishToMavenLocal
    // To publish to a remote repository, configure repositories in the publishing block.
}
