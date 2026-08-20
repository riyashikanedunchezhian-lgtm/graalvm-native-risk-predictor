plugins {
    id("java-gradle-plugin")
}

dependencies {
    implementation(project(":native-risk-core"))
    testImplementation(libs.junit.jupiter)
}

gradlePlugin {
    plugins {
        create("nativeCompatibilityPlugin") {
            id = "io.nativerisk.native-compatibility"
            implementationClass = "io.nativerisk.gradle.NativeCompatibilityPlugin"
            displayName = "GraalVM Native Image Compatibility Checker"
            description = "Analyzes compiled bytecode and dependencies for GraalVM Native Image compatibility risk before you run native-image."
        }
    }
}

sourceSets {
    create("functionalTest") {
        java.srcDir("src/functionalTest/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    dependsOn(functionalTest)
}
