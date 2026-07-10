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

import org.e2immu.gradleplugin.task.AnalyzerTask;
import org.e2immu.gradleplugin.task.WriteInputConfigurationTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AnalyzerPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(AnalyzerPlugin.class);

    @Override
    public void apply(Project project) {
        if (project.getExtensions().findByName(AnalyzerExtension.ANALYZER_EXTENSION_NAME) == null) {
            Map<String, ActionBroadcast<AnalyzerProperties>> actionBroadcastMap = new HashMap<>();
            addExtensions(project, actionBroadcastMap);

            LOGGER.debug("Adding " + AnalyzerExtension.ANALYZER_TASK_NAME + " task to " + project);
            project.getTasks().register(AnalyzerExtension.ANALYZER_TASK_NAME,
                    (Class<? extends ConventionTask>) AnalyzerTask.class, t -> {
                        t.setDescription("Analyses " + project + " with the e2immu analyzer.");
                        configureProperties(t, project, actionBroadcastMap);
                        // the forked analyzer resolves relative source-set paths against this directory (the same
                        // working directory AnalyzerPropertyComputer used to compute those paths)
                        t.getConventionMapping().map("workingDirectory", () -> {
                            String wd = project.getExtensions().getByType(AnalyzerExtension.class).workingDirectory;
                            return wd == null || wd.isBlank() ? project.getProjectDir() : new File(wd);
                        });
                        dependOnCompileTasks(t, project);
                    });

            LOGGER.debug("Adding " + AnalyzerExtension.WRITE_INPUT_CONFIGURATION_TASK_NAME + " task to " + project);
            project.getTasks().register(AnalyzerExtension.WRITE_INPUT_CONFIGURATION_TASK_NAME,
                    (Class<? extends ConventionTask>) WriteInputConfigurationTask.class, t -> {
                        t.setDescription("Writes out the input configuration of the project to a json file");
                        configureProperties(t, project, actionBroadcastMap);
                        t.getConventionMapping().map("outputFile", () -> {
                            File buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
                            return new File(buildDir, "inputConfiguration.json");
                        });
                        dependOnCompileTasks(t, project);
                    });
        }
    }

    private void addExtensions(Project project, Map<String, ActionBroadcast<AnalyzerProperties>> actionBroadcastMap) {
        project.getAllprojects().forEach(p -> {
            LOGGER.debug("Adding " + AnalyzerExtension.ANALYZER_EXTENSION_NAME + " extension to " + p);
            ActionBroadcast<AnalyzerProperties> actionBroadcast = new ActionBroadcast<>();
            actionBroadcastMap.put(project.getPath(), actionBroadcast);
            p.getExtensions().create(AnalyzerExtension.ANALYZER_EXTENSION_NAME, AnalyzerExtension.class, actionBroadcast);
        });
    }

    // this will call the AnalyzerPropertyComputer to populate the 'properties' of the task just before running it
    private void configureProperties(ConventionTask task, Project project, Map<String, ActionBroadcast<AnalyzerProperties>> actionBroadcastMap) {
        ConventionMapping conventionMapping = task.getConventionMapping();
        conventionMapping.map("properties",
                () -> new AnalyzerPropertyComputer(actionBroadcastMap, project).computeProperties());
    }

    // the analyzer needs the compiled classes on the classpath, so run after the (non-skipped) Java compilations
    private void dependOnCompileTasks(ConventionTask task, Project project) {
        Callable<Iterable<? extends Task>> compileTasks = () -> project.getAllprojects().stream()
                .filter(p -> p.getPlugins().hasPlugin(JavaPlugin.class)
                             && !p.getExtensions().getByType(AnalyzerExtension.class).skipProject)
                .map(p -> p.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME))
                .collect(Collectors.toList());
        task.dependsOn(compileTasks);
    }
}
