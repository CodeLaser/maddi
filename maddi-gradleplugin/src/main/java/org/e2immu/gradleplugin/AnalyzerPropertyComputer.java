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
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.run.config.GeneralConfiguration;
import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.analyzer.run.main.Main;
import org.e2immu.gradleplugin.inputconfig.ComputeDependencies;
import org.e2immu.gradleplugin.inputconfig.ComputeSourceSets;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.op.Linearize;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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
    public static final String PREFIX = "e2immu-analyser.";

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
        org.e2immu.analyzer.run.config.Configuration configuration = computeConfiguration(project, extension);
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
            LOGGER.debug("Skipping collecting Analyser properties on: " +
                         skippedChildProjects.stream().map(Project::toString).collect(Collectors.joining(", ")));
        }

        // recurse
        for (Project childProject : enabledChildProjects) {
            String moduleId = childProject.getPath();
            String modulePrefix = !prefix.isEmpty() ? (prefix + "." + moduleId) : moduleId;
            computeProperties(childProject, properties, modulePrefix);
        }*/
    }

    public org.e2immu.analyzer.run.config.Configuration computeConfiguration(Project project, AnalyzerExtension extension) {
        LanguageConfiguration languageConfiguration = new LanguageConfigurationImpl(true);

        // general
        Map<String, String> generalMap = makeGeneralConfigMap(project, extension);
        GeneralConfiguration generalConfiguration = Main.generalConfiguration(generalMap);
        // Annotated API
        Map<String, String> aapiMap = makeAnnotatedAPIMap(extension);
        AnnotatedAPIConfiguration annotatedAPIConfiguration = Main.annotatedAPIConfiguration(aapiMap);
        // Input
        InputConfiguration inputConfiguration = makeInputConfiguration(project, extension);

        return new org.e2immu.analyzer.run.config.Configuration.Builder()
                .setAnnotatedAPIConfiguration(annotatedAPIConfiguration)
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

        Set<String> excludeFromClasspath = extension.excludeFromClasspath == null || extension.excludeFromClasspath.isBlank() ? Set.of() :
                Arrays.stream(extension.excludeFromClasspath.split("[;,]\\s*")).collect(Collectors.toUnmodifiableSet());
        ComputeSourceSets computeSourceSets = new ComputeSourceSets(absoluteWorkingDirectory);
        ComputeSourceSets.Result result = computeSourceSets.compute(project, extension.sourcePackages,
                extension.testSourcePackages, excludeFromClasspath);
        makeJavaModules(extension.jmods).forEach(set -> result.sourceSetsByName().put(set.name(), set));

        G<String> graph = new ComputeDependencies().go(result);
        List<String> linearization = Linearize.linearize(graph).asList(String::compareToIgnoreCase);
        LOGGER.info("Graph: {}", graph);
        LOGGER.info("Linearization:\n  {}\n", String.join("\n  ", linearization));
        Map<String, SourceSet> allSourceSetsByName = result.allSourceSetsByName();
        for (String name : linearization) {
            Map<V<String>, Long> edges = graph.edges(new V<>(name));
            Set<SourceSet> dependencies = edges == null ? Set.of() : edges.keySet()
                    .stream().map(v -> allSourceSetsByName.get(v.t()))
                    .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
            SourceSet sourceSet = allSourceSetsByName.get(name);
            if (sourceSet == null) {
                LOGGER.warn("Don't know source set {}", name);
            } else {
                SourceSet set = sourceSet.withDependencies(dependencies);
                if (!set.externalLibrary()) builder.addSourceSets(set);
                else builder.addClassPathParts(set);
            }
        }
        return builder.build();
    }

    private List<SourceSet> makeJavaModules(String jmodsString) {
        List<SourceSet> sets = new ArrayList<>();
        Set<String> jmods = JavaModules.jmodsFromString(jmodsString);
        for (String jmod : jmods) {
            if (!jmod.isBlank()) {
                SourceSet set = new SourceSetImpl(jmod, null,
                        URI.create("jmod:" + jmod),
                        null, false, true, true, true, false,
                        null, null);
                sets.add(set);
            }
        }
        return sets;
    }

    private static Map<String, String> makeAnnotatedAPIMap(AnalyzerExtension extension) {
        // Annotated API
        // use case 1
        Map<String, String> kvMap = new HashMap<>();
        if (extension.analyzedAnnotatedApiDirs != null) {
            kvMap.put(Main.ANALYZED_ANNOTATED_API_DIRS, extension.analyzedAnnotatedApiDirs);
        }
        // use case 2
        if (extension.analyzedAnnotatedApiTargetDir != null) {
            kvMap.put(Main.ANALYZED_ANNOTATED_API_TARGET_DIR, extension.analyzedAnnotatedApiTargetDir);
        }
        // use case 3
        if (extension.annotatedApiTargetDir != null) {
            kvMap.put(Main.ANNOTATED_API_TARGET_DIR, extension.annotatedApiTargetDir);
        }
        if (extension.annotatedApiTargetPackage != null) {
            kvMap.put(Main.ANNOTATED_API_TARGET_PACKAGE, extension.annotatedApiTargetPackage);
        }
        if (extension.annotatedApiPackages != null) {
            kvMap.put(Main.ANNOTATED_API_PACKAGES, extension.annotatedApiPackages);
        }
        return kvMap;
    }

    private static @NotNull Map<String, String> makeGeneralConfigMap(Project project,
                                                                     AnalyzerExtension extension) {
        Map<String, String> generalMap = new HashMap<>();
        generalMap.put(Main.INCREMENTAL_ANALYSIS, "" + extension.incrementalAnalysis);
        String analysisResultsDir;
        if (extension.analysisResultsDir != null) {
            analysisResultsDir = extension.analysisResultsDir;
        } else {
            // default value: "${build.dir}/e2immu"
            File buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
            analysisResultsDir = new File(buildDir, "e2immu").getAbsolutePath();
        }
        generalMap.put(Main.ANALYSIS_RESULTS_DIR, analysisResultsDir);
        generalMap.put(Main.PARALLEL, "" + extension.parallel);
        generalMap.put(Main.ANALYSIS_STEPS, extension.analysisSteps);
        generalMap.put(Main.DEBUG, extension.debugTargets);
        generalMap.put(Main.QUIET, "" + extension.quiet);
        return generalMap;
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
