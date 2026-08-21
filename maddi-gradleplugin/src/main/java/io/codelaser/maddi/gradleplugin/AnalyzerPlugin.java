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

package io.codelaser.maddi.gradleplugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.gradleplugin.inputconfig.SourceFactsFile;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.gradleplugin.task.AnalyzerTask;
import io.codelaser.maddi.gradleplugin.task.WriteInputConfigurationTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.attributes.Category;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The maddi Gradle plugin. Registers the {@code maddi-analyzer} and {@code maddi-write-input-configuration} tasks
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

        publishSourceElements(project);

        LOGGER.debug("Adding {} task to {}", AnalyzerExtension.ANALYZER_TASK_NAME, project);
        project.getTasks().register(AnalyzerExtension.ANALYZER_TASK_NAME, AnalyzerTask.class, task -> {
            task.setGroup("maddi");
            task.setDescription("Analyses " + project + " with the maddi analyzer.");
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
                    task.setGroup("maddi");
                    task.setDescription("Writes out the input configuration of the project to a json file");
                    task.getConfigurationJson().set(configurationJson);
                    task.getOutputFile().set(project.getLayout().getBuildDirectory().file("inputConfiguration.json"));
                    dependOnCompileTasks(task, project);
                });
    }

    /**
     * Publish this project's main source <em>directories</em> as a consumable variant, so a project that depends
     * on it can co-analyze its sources rather than read its jar. That distinction decides real results: an
     * interface reached only as a jar never enters the abstract-method batch, so nothing an implementation
     * computed (modification, independence, eventual immutability) can travel up to it -- and by the hierarchy
     * rule an undecided or mutable supertype then drags every implementation down with it.
     * <p>
     * {@code Category} is what makes the variant selectable: the normal {@code apiElements}/{@code
     * runtimeElements} variants declare {@code Category=library}, so a request for {@code maddi-sources} is
     * incompatible with them and can only match this one. The artifacts go in through the {@code Provider}
     * overload, not a fixed list: a build script sets its {@code srcDirs} after applying the plugin, so
     * enumerating them here and now would publish the defaults instead of what the user configured.
     */
    private void publishSourceElements(Project project) {
        project.getPlugins().withType(JavaPlugin.class, jp ->
                project.getConfigurations().consumable(AnalyzerExtension.SOURCE_ELEMENTS_CONFIGURATION_NAME, conf -> {
                    conf.setDescription("Source directories of " + project + ", for co-analysis by the maddi analyzer.");
                    conf.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                            project.getObjects().named(Category.class, AnalyzerExtension.SOURCES_CATEGORY));
                    // ⭐ THE FACTS FILE RIDES ALONG WITH THE SOURCE DIRECTORIES, and the provider is what
                    // makes that legal: it runs when a CONSUMER RESOLVES this variant, which is after this
                    // project has been configured, so the file exists by the time it is read. See
                    // SourceFactsFile for the three shapes that were rejected -- in particular the one that
                    // publishes a TASK's output, which does not exist at the consumer's configuration time.
                    conf.getOutgoing().artifacts(project.provider(() -> {
                        List<Object> artifacts = new ArrayList<>(mainSourceDirectories(project));
                        File facts = SourceFactsFile.write(project);
                        if (facts != null) artifacts.add(facts);
                        return artifacts;
                    }));
                }));
    }

    /** The existing, readable source directories of the main source set: Java, plus Kotlin when that plugin is on. */
    private static List<File> mainSourceDirectories(Project project) {
        JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
        if (javaPluginExtension == null) return List.of();
        SourceSet main = javaPluginExtension.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (main == null) return List.of();
        Set<File> srcDirs = new LinkedHashSet<>(main.getAllJava().getSrcDirs());
        Object kotlin = ((ExtensionAware) main).getExtensions().findByName("kotlin");
        if (kotlin instanceof SourceDirectorySet kotlinDirs) {
            srcDirs.addAll(kotlinDirs.getSrcDirs());
        }
        // a directory that is not there would be published as a missing artifact, which fails resolution
        return srcDirs.stream().filter(File::canRead).toList();
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

    /**
     * Run after the compilations, because the analyzer needs the compiled classes on the class path — and needs
     * them for <em>every</em> source set it is about to read, not just {@code main}.
     * <p>
     * The openjdk front end drives javac one source set at a time and resolves each set's references into the sets
     * it depends on through their <b>class files</b> (javac knows nothing of maddi's CST). {@code compileJava} alone
     * therefore left {@code test} — which {@code ComputeSourceSets} hands over just like {@code main}, along with
     * any custom source set such as {@code functionalTest} — with no guarantee at all: analysing a project whose
     * test classes had never been built dropped every compilation unit that referenced them, as tolerable warnings.
     * Each source set's {@code classes} task covers all of them; the explicit {@code compileKotlin} dependency
     * stays alongside it rather than being assumed to ride in on {@code classes}.
     * <p>
     * {@code all} rather than a snapshot loop: a build script may register a source set after the plugin is applied,
     * and one we did not wire is exactly one whose classes may be missing.
     */
    private void dependOnCompileTasks(Task task, Project project) {
        project.getPlugins().withType(JavaPlugin.class, jp -> {
            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            javaPluginExtension.getSourceSets().all(sourceSet ->
                    task.dependsOn(project.getTasks().named(sourceSet.getClassesTaskName())));
        });
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
        return new File(buildDir, "maddi");
    }

    private static String toJson(Configuration configuration) {
        try {
            return JsonStreaming.objectMapper().writeValueAsString(configuration);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize the maddi configuration", e);
        }
    }
}
