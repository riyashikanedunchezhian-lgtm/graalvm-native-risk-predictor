package io.nativerisk.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional test using Gradle TestKit against a minimal generated
 * project (not the full samples/ projects, to keep this fast and
 * self-contained). See samples/ for richer manual-testing fixtures
 * that exercise specific detector patterns.
 */
class NativeCompatibilityPluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void taskRunsAndProducesReports() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'sample'");
        writeFile("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.nativerisk.native-compatibility'
                }
                """);

        Path srcDir = projectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Sample.java"), """
                package com.example;
                public class Sample {
                    public void doWork() throws Exception {
                        Class.forName("com.example.Other");
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("nativeCompatibilityCheck")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("Compatibility Score"));
        assertTrue(Files.exists(projectDir.resolve("build/reports/native-risk/report.html")));
        assertTrue(Files.exists(projectDir.resolve("build/reports/native-risk/report.json")));
    }

    private void writeFile(String relativePath, String content) throws IOException {
        Path path = projectDir.resolve(relativePath);
        Files.createDirectories(path.getParent() == null ? projectDir : path.getParent());
        Files.writeString(path, content);
    }
}
