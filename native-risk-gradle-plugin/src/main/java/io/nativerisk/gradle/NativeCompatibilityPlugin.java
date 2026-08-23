package io.nativerisk.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Registers the `nativeCompatibilityCheck` task and wires it to run
 * after `compileJava`, per the "triggered after compilation" design
 * in docs/architecture.md. Applying `java` (or a plugin that applies
 * it, e.g. `java-library`) is a prerequisite -- this plugin does not
 * apply it itself, to avoid surprising projects that configure the
 * Java plugin in a specific way.
 */
public class NativeCompatibilityPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            JavaCompile compileJava = (JavaCompile) project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName());

            NativeCompatibilityCheckTask checkTask = project.getTasks().create(
                    "nativeCompatibilityCheck",
                    NativeCompatibilityCheckTask.class,
                    task -> {
                        task.setGroup("verification");
                        task.setDescription("Analyzes compiled bytecode and dependencies for GraalVM Native Image compatibility risk.");
                        task.getCompiledClassesDir().set(compileJava.getDestinationDirectory());
                        task.getRuntimeClasspath().from(mainSourceSet.getRuntimeClasspath());
                        task.getReportDirectory().set(project.getLayout().getBuildDirectory().dir("reports/native-risk"));
                        task.dependsOn(compileJava);
                    }
            );

            project.getLogger().info("native-risk: registered nativeCompatibilityCheck task, depends on {}", compileJava.getName());
        });

        project.getPlugins().apply(JavaPlugin.class);
    }
}
