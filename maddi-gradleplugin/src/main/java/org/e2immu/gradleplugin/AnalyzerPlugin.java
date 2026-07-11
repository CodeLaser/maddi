/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.gradleplugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.gradleplugin.task.AnalyzerTask;
import org.e2immu.gradleplugin.task.WriteInputConfigurationTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The maddi Gradle plugin. Registers the {@code e2immu-analyzer} and {@code e2immu-write-input-configuration} tasks
 * as modern, lazily-configured, configuration-cache-compatible tasks. Everything that needs the {@link Project}
 * model (source sets, resolved classpath) is read at configuration time into a serialized {@link Configuration}
 * (a plain String), so nothing {@code Project}-bound survives into task execution.
 */
public class AnalyzerPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(AnalyzerPlugin.class);

    private static final String KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm";
    private static final String COMPILE_KOTLIN_TASK_NAME = "compileKotlin";

    @Override
    public void apply(Project project) {
        if (project.getExtensions().findByName(AnalyzerExtension.ANALYZER_EXTENSION_NAME) != null) {
            return;
        }
        ActionBroadcast<AnalyzerProperties> actionBroadcast = new ActionBroadcast<>();
        Map<String, ActionBroadcast<AnalyzerProperties>> actionBroadcastMap = new HashMap<>();
        actionBroadcastMap.put(project.getPath(), actionBroadcast);
        project.getExtensions().create(AnalyzerExtension.ANALYZER_EXTENSION_NAME, AnalyzerExtension.class, actionBroadcast);

        // Compute the run-config Configuration lazily; resolved at configuration/store time (Project still
        // available) and mapped to a plain JSON String, which is what the tasks and the forked worker consume.
        Provider<String> configurationJson = project.provider(() -> {
            AnalyzerExtension extension = project.getExtensions().getByType(AnalyzerExtension.class);
            Configuration configuration = new AnalyzerPropertyComputer(actionBroadcastMap, project)
                    .computeConfiguration(project, extension);
            return toJson(configuration);
        });

        LOGGER.debug("Adding {} task to {}", AnalyzerExtension.ANALYZER_TASK_NAME, project);
        project.getTasks().register(AnalyzerExtension.ANALYZER_TASK_NAME, AnalyzerTask.class, task -> {
            task.setGroup("e2immu");
            task.setDescription("Analyses " + project + " with the e2immu analyzer.");
            task.getConfigurationJson().set(configurationJson);
            // the forked analyzer resolves relative source-set paths against this directory
            task.getWorkingDirectory().fileProvider(project.provider(() -> workingDirectory(project)));
            task.getAnalysisResultsDir().fileProvider(project.provider(() -> analysisResultsDir(project)));
            wireAnalyzedInputs(task, project);
            dependOnCompileTasks(task, project);
        });

        LOGGER.debug("Adding {} task to {}", AnalyzerExtension.WRITE_INPUT_CONFIGURATION_TASK_NAME, project);
        project.getTasks().register(AnalyzerExtension.WRITE_INPUT_CONFIGURATION_TASK_NAME,
                WriteInputConfigurationTask.class, task -> {
                    task.setGroup("e2immu");
                    task.setDescription("Writes out the input configuration of the project to a json file");
                    task.getConfigurationJson().set(configurationJson);
                    task.getOutputFile().set(project.getLayout().getBuildDirectory().file("inputConfiguration.json"));
                    dependOnCompileTasks(task, project);
                });
    }

    /** The analyzer's source files and compile classpath, for up-to-date checking. Both are Gradle-managed lazy
     * file collections (no eager resolution), so this stays configuration-cache compatible. {@code getAllSource()}
     * includes the Kotlin sources too when the Kotlin plugin is applied. */
    private void wireAnalyzedInputs(AnalyzerTask task, Project project) {
        project.getPlugins().withType(JavaPlugin.class, jp -> {
            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            for (SourceSet sourceSet : javaPluginExtension.getSourceSets()) {
                task.getAnalyzedInputs().from(sourceSet.getAllSource());
                task.getAnalyzedInputs().from(sourceSet.getCompileClasspath());
            }
        });
    }

    // the analyzer needs the compiled classes on the classpath, so run after the compilations (Java, and Kotlin
    // when the Kotlin JVM plugin is applied)
    private void dependOnCompileTasks(Task task, Project project) {
        project.getPlugins().withType(JavaPlugin.class,
                jp -> task.dependsOn(project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME)));
        project.getPlugins().withId(KOTLIN_JVM_PLUGIN_ID,
                kp -> task.dependsOn(project.getTasks().named(COMPILE_KOTLIN_TASK_NAME)));
    }

    private static File workingDirectory(Project project) {
        String wd = project.getExtensions().getByType(AnalyzerExtension.class).workingDirectory;
        return wd == null || wd.isBlank() ? project.getProjectDir() : new File(wd);
    }

    private static File analysisResultsDir(Project project) {
        String dir = project.getExtensions().getByType(AnalyzerExtension.class).analysisResultsDir;
        if (dir != null && !dir.isBlank()) {
            return new File(dir);
        }
        File buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
        return new File(buildDir, "e2immu");
    }

    private static String toJson(Configuration configuration) {
        try {
            return JsonStreaming.objectMapper().writeValueAsString(configuration);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize the e2immu configuration", e);
        }
    }
}
