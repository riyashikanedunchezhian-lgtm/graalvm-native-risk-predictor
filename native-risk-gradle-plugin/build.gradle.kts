plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    implementation(project(":native-risk-core"))
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("nativeRisk") {
            id = "com.example.native-risk"
            implementationClass = "com.example.NativeRiskPlugin"
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
    
    dependsOn("jar", "publishToMavenLocal")
}

tasks.check {
    dependsOn(functionalTest)
}

tasks.named("functionalTest") {
    dependsOn("jar", "publishToMavenLocal")
}