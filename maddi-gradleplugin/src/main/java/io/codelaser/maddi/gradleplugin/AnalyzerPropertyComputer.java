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
import io.codelaser.maddi.aapi.parser.AnalysisHintsConfiguration;
import io.codelaser.maddi.run.config.GeneralConfiguration;
import io.codelaser.maddi.run.config.util.JavaModules;
import io.codelaser.maddi.run.config.util.PluginInputConfiguration;
import io.codelaser.maddi.run.main.PluginOptions;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.run.main.Main;
import io.codelaser.maddi.gradleplugin.inputconfig.ComputeDependencies;
import io.codelaser.maddi.gradleplugin.inputconfig.ComputeSourceSets;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.runtime.LanguageConfiguration;
import io.codelaser.maddi.cst.impl.runtime.LanguageConfigurationImpl;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.graph.op.Linearize;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Property names are identical to those of the CLI (.cli.Main). In the system properties,
 * they have to be prefixed by the PREFIX defined in this class.
 */
public record AnalyzerPropertyComputer(
        Map<String, ActionBroadcast<AnalyzerProperties>> actionBroadcastMap,
        Project targetProject) {

    private static final Logger LOGGER = Logging.getLogger(AnalyzerPropertyComputer.class);
    public static final String PREFIX = "e2immu-analyzer.";

    public static final String E2IMMU_CONFIGURATION = "configuration.json";

    public Map<String, Object> computeProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        computeProperties(targetProject, properties, "");

        return properties;
    }

    private void computeProperties(Project project, Map<String, Object> properties, String prefix) {
        AnalyzerExtension extension = project.getExtensions().getByType(AnalyzerExtension.class);
        if (extension.skipProject) {
            return;
        }
        io.codelaser.maddi.run.config.Configuration configuration = computeConfiguration(project, extension);
        try {
            String json = JsonStreaming.objectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(configuration);
            LOGGER.info("Configuration for project {}: {}", project.getDisplayName(), json);
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
        try {
            String configurationJson = JsonStreaming.objectMapper().writeValueAsString(configuration);
            properties.put(E2IMMU_CONFIGURATION, configurationJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ActionBroadcast<AnalyzerProperties> actionBroadcast = actionBroadcastMap.get(project.getPath());
        if (actionBroadcast != null) {
            AnalyzerProperties analyzerProperties = new AnalyzerProperties(properties);
            actionBroadcast.execute(analyzerProperties);
        }

        // with the highest priority, override directly for this project from the system properties
        if (project.equals(targetProject)) {
            addSystemProperties(properties);
        }
        // convert all the properties from subprojects into dot-notated properties
        // flattenProperties(rawProperties, prefix, properties);
        /*
        LOGGER.debug("Resulting map is " + properties);

        List<Project> enabledChildProjects = project.getChildProjects().values().stream()
                .filter(p -> !p.getExtensions().getByType(AnalyzerExtension.class).skipProject)
                .toList();

        List<Project> skippedChildProjects = project.getChildProjects().values().stream()
                .filter(p -> p.getExtensions().getByType(AnalyzerExtension.class).skipProject)
                .toList();

        if (!skippedChildProjects.isEmpty()) {
            LOGGER.debug("Skipping collecting Analyzer properties on: " +
                         skippedChildProjects.stream().map(Project::toString).collect(Collectors.joining(", ")));
        }

        // recurse
        for (Project childProject : enabledChildProjects) {
            String moduleId = childProject.getPath();
            String modulePrefix = !prefix.isEmpty() ? (prefix + "." + moduleId) : moduleId;
            computeProperties(childProject, properties, modulePrefix);
        }*/
    }

    public io.codelaser.maddi.run.config.Configuration computeConfiguration(Project project, AnalyzerExtension extension) {
        LanguageConfiguration languageConfiguration = new LanguageConfigurationImpl(true);

        // general
        Map<String, String> generalMap = makeGeneralConfigMap(project, extension);
        GeneralConfiguration generalConfiguration = Main.generalConfiguration(generalMap);
        // AnalysisHints
        Map<String, String> aapiMap = makeAnalysisHintsMap(extension);
        AnalysisHintsConfiguration analysisHintsConfiguration = Main.analysisHintsConfiguration(aapiMap);
        // Input
        InputConfiguration inputConfiguration = makeInputConfiguration(project, extension);

        return new io.codelaser.maddi.run.config.Configuration.Builder()
                .setAnalysisHintsConfiguration(analysisHintsConfiguration)
                .setGeneralConfiguration(generalConfiguration)
                .setLanguageConfiguration(languageConfiguration)
                .setInputConfiguration(inputConfiguration)
                .build();
    }

    private InputConfiguration makeInputConfiguration(Project project, AnalyzerExtension extension) {
        LOGGER.info("Computing input configuration of project {}", project.getDisplayName());

        InputConfiguration.Builder builder = new InputConfigurationImpl.Builder();
        builder.setAlternativeJREDirectory(extension.jre);
        Path workingDirectory = extension.workingDirectory == null || extension.workingDirectory.isBlank()
                ? project.getLayout().getProjectDirectory().getAsFile().toPath()
                : Path.of(extension.workingDirectory);
        builder.setWorkingDirectory(workingDirectory.toString());
        Path absoluteWorkingDirectory = workingDirectory.toAbsolutePath();

        Set<String> excludeFromClasspath = PluginOptions.splitToSet(extension.excludeFromClasspath);
        ComputeSourceSets computeSourceSets = new ComputeSourceSets(absoluteWorkingDirectory);
        ComputeSourceSets.Result result = computeSourceSets.compute(project, extension.sourcePackages,
                extension.testSourcePackages, excludeFromClasspath);
        List<SourceSet> javaModules = JavaModules.javaModuleSourceSets(extension.jmods);
        javaModules.forEach(set -> result.sourceSetsByName().put(set.name(), set));

        G<String> graph = new ComputeDependencies().go(result);
        LOGGER.info("Graph: {}", graph);
        PluginInputConfiguration.emit(builder, graph, result.allSourceSetsByName(), javaModules, LOGGER::info);
        return builder.build();
    }

    private static Map<String, String> makeAnalysisHintsMap(AnalyzerExtension extension) {
        return PluginOptions.analysisHintsMap(extension.preloadAnalysisResultsDirs,
                extension.analysisResultsTargetDir, extension.updatedHintsDir, extension.updatedHintsPackage,
                extension.hintsPackages);
    }

    private static @NotNull Map<String, String> makeGeneralConfigMap(Project project,
                                                                     AnalyzerExtension extension) {
        // default results directory: "${build.dir}/e2immu"
        File buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
        return PluginOptions.generalConfigMap(extension.incrementalAnalysis, extension.analysisResultsDir,
                new File(buildDir, "e2immu"), extension.parallel, extension.analysisSteps, extension.debugTargets,
                extension.quiet, extension.warnNearMisses);
    }

    private static void addSystemProperties(Map<String, Object> properties) {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(PREFIX)) {
                LOGGER.debug("Overwriting property from system: {}", key);
                String strippedKey = key.substring(PREFIX.length());
                properties.put(strippedKey, entry.getValue().toString());
            }
        }
    }

}
