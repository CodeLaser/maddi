package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfiguration;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.analyzer.run.config.GeneralConfiguration;
import org.e2immu.analyzer.run.config.util.ComputeDependencies;
import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.analyzer.run.main.Main;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.op.Linearize;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


public abstract class CommonMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "jre", defaultValue = "")
    private String jre;

    @Parameter(property = "workingDirectory", defaultValue = "${project.basedir}")
    private String workingDirectory;

    @Parameter(property = "excludeFromClasspath", defaultValue = "")
    private String excludeFromClasspath;

    @Parameter(property = "jmods", defaultValue = "java.se")
    private String jmods;

    @Parameter(property = "testSourcePackages", defaultValue = "")
    private String testSourcePackages;

    @Parameter(property = "sourcePackages", defaultValue = "")
    private String sourcePackages;

    @Parameter(property = "sourceEncoding", defaultValue = "UTF-8")
    private String sourceEncoding;

    // --- general analysis configuration (mirrors the Gradle plugin's extension) ---

    @Parameter(property = "analysisResultsDir", defaultValue = "")
    private String analysisResultsDir;

    @Parameter(property = "analysisSteps", defaultValue = "")
    private String analysisSteps;

    @Parameter(property = "incrementalAnalysis", defaultValue = "false")
    private boolean incrementalAnalysis;

    @Parameter(property = "parallel", defaultValue = "true")
    private boolean parallel;

    @Parameter(property = "quiet", defaultValue = "false")
    private boolean quiet;

    @Parameter(property = "debug", defaultValue = "")
    private String debug;

    // --- analysis-hints configuration (the three use cases) ---

    @Parameter(property = "preloadAnalysisResultsDirs", defaultValue = "")
    private String preloadAnalysisResultsDirs;

    @Parameter(property = "analysisResultsTargetDir", defaultValue = "")
    private String analysisResultsTargetDir;

    @Parameter(property = "updatedHintsDir", defaultValue = "")
    private String updatedHintsDir;

    @Parameter(property = "updatedHintsPackage", defaultValue = "")
    private String updatedHintsPackage;

    @Parameter(property = "hintsPackages", defaultValue = "")
    private String hintsPackages;

    @Component
    private ProjectDependenciesResolver dependenciesResolver;

    /**
     * Assemble the full {@link Configuration} (general + analysis-hints + language + input) the way the Gradle
     * plugin's {@code AnalyzerPropertyComputer} does, so both plugins hand an identical object to {@code RunAnalyzer}.
     */
    protected Configuration computeConfiguration() throws DependencyResolutionException {
        LanguageConfiguration languageConfiguration = new LanguageConfigurationImpl(true);
        GeneralConfiguration generalConfiguration = Main.generalConfiguration(makeGeneralConfigMap());
        AnalysisHintsConfiguration analysisHintsConfiguration = Main.analysisHintsConfiguration(makeAnalysisHintsMap());
        InputConfiguration inputConfiguration = makeInputConfiguration();
        return new Configuration.Builder()
                .setAnalysisHintsConfiguration(analysisHintsConfiguration)
                .setGeneralConfiguration(generalConfiguration)
                .setLanguageConfiguration(languageConfiguration)
                .setInputConfiguration(inputConfiguration)
                .build();
    }

    private Map<String, String> makeGeneralConfigMap() {
        Map<String, String> generalMap = new HashMap<>();
        generalMap.put(Main.INCREMENTAL_ANALYSIS, "" + incrementalAnalysis);
        String resultsDir = analysisResultsDir != null && !analysisResultsDir.isBlank() ? analysisResultsDir
                : new File(project.getBuild().getDirectory(), "e2immu").getAbsolutePath();
        generalMap.put(Main.ANALYSIS_RESULTS_DIR, resultsDir);
        generalMap.put(Main.PARALLEL, "" + parallel);
        if (analysisSteps != null && !analysisSteps.isBlank()) generalMap.put(Main.ANALYSIS_STEPS, analysisSteps);
        if (debug != null && !debug.isBlank()) generalMap.put(Main.DEBUG, debug);
        generalMap.put(Main.QUIET, "" + quiet);
        return generalMap;
    }

    private Map<String, String> makeAnalysisHintsMap() {
        Map<String, String> kvMap = new HashMap<>();
        if (preloadAnalysisResultsDirs != null && !preloadAnalysisResultsDirs.isBlank()) {
            kvMap.put(Main.PRELOAD_ANALYSIS_RESULTS_DIRS, preloadAnalysisResultsDirs);
        }
        if (analysisResultsTargetDir != null && !analysisResultsTargetDir.isBlank()) {
            kvMap.put(Main.ANALYSIS_RESULTS_TARGET_DIR, analysisResultsTargetDir);
        }
        if (updatedHintsDir != null && !updatedHintsDir.isBlank()) {
            kvMap.put(Main.UPDATED_HINTS_DIR, updatedHintsDir);
        }
        if (updatedHintsPackage != null && !updatedHintsPackage.isBlank()) {
            kvMap.put(Main.UPDATED_HINTS_PACKAGE, updatedHintsPackage);
        }
        if (hintsPackages != null && !hintsPackages.isBlank()) {
            kvMap.put(Main.HINTS_PACKAGES, hintsPackages);
        }
        return kvMap;
    }

    protected InputConfiguration makeInputConfiguration() throws DependencyResolutionException {
        InputConfiguration.Builder builder = new InputConfigurationImpl.Builder();
        builder.setAlternativeJREDirectory(jre);
        builder.setWorkingDirectory(workingDirectory);

        Set<String> excludeFromClasspathSet = excludeFromClasspath == null || excludeFromClasspath.isBlank() ? Set.of() :
                Arrays.stream(excludeFromClasspath.split("[;,]\\s*")).collect(Collectors.toUnmodifiableSet());
        ComputeDependencies.SourceSetDependencies result = new ComputeSourceSets(dependenciesResolver, project,
                session, getLog()).compute(sourceEncoding, sourcePackages, testSourcePackages, excludeFromClasspathSet);

        List<SourceSet> javaModules = makeJavaModules(jmods);
        javaModules.forEach(set -> result.sourceSetsByName().put(set.name(), set));

        G<String> graph = new ComputeDependencies(s -> getLog().debug(s)).go(result);
        List<String> linearization = Linearize.linearize(graph).asList(String::compareToIgnoreCase);
        if (getLog().isDebugEnabled()) {
            getLog().debug("Graph: " + graph);
            getLog().debug("Linearization:\n  " + String.join("\n  ", linearization) + "\n");
        }
        Set<String> emitted = new HashSet<>();
        for (String name : linearization) {
            Map<V<String>, Long> edges = graph.edges(new V<>(name));
            List<SourceSet> dependencies = edges == null ? List.of() : edges.keySet()
                    .stream().map(v -> result.sourceSetsByName().get(v.t()))
                    .filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
            SourceSet sourceSet = result.sourceSetsByName().get(name);
            if (sourceSet == null) {
                getLog().warn("Don't know source set " + name);
            } else {
                SourceSet set = sourceSet.withDependencies(dependencies);
                if (!set.externalLibrary()) builder.addSourceSets(set);
                else builder.addClassPathParts(set);
                emitted.add(name);
            }
        }
        // The dependency graph typically only reaches java.base, so the rest of the requested jmod closure
        // (java.se: java.logging, java.xml, java.desktop, java.sql, ...) would be dropped. Non-modular sources
        // need the whole JDK visible, so force every requested JDK module onto the classpath regardless.
        for (SourceSet jmodSet : javaModules) {
            if (emitted.add(jmodSet.name())) {
                builder.addClassPathParts(jmodSet);
            }
        }
        return builder.build();
    }

    private List<SourceSet> makeJavaModules(String jmodsString) {
        List<SourceSet> sets = new ArrayList<>();
        Set<String> jmods = JavaModules.jmodsFromString(jmodsString);
        for (String jmod : jmods) {
            if (!jmod.isBlank()) {
                SourceSet set = new SourceSetImpl.Builder().setName(jmod)
                        .setUri(URI.create("jmod:" + jmod))
                        .setLibrary(true).setExternalLibrary(true).setPartOfJdk(true).setModule(true).build();
                sets.add(set);
            }
        }
        return sets;
    }

    protected record ParseSourcesResult(ParseResult parseResult,
                                        JavaInspector javaInspector,
                                        InputConfiguration inputConfiguration) {
    }

    /**
     * Parse the project's sources with the in-house parser (no {@code --add-exports} needed). Used by the auxiliary
     * mojos ({@code statistics}, {@code write-analysis-hints}); the {@code run} mojo goes through {@code RunAnalyzer}.
     */
    protected ParseSourcesResult parseSources() throws DependencyResolutionException, IOException {
        InputConfiguration inputConfiguration = makeInputConfiguration();
        JavaInspector javaInspector = new JavaInspectorImpl(true, true);

        InputConfiguration withSupport = inputConfiguration.withE2ImmuSupportFromClasspath().withDefaultModules();
        getLog().info("Working directory: " + withSupport.workingDirectory());
        javaInspector.initialize(withSupport);

        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();
        Summary summary = javaInspector.parse(parseOptions);
        return new ParseSourcesResult(summary.parseResult(), javaInspector, inputConfiguration);
    }

    protected static String packagePrefixGenerator(String packagePrefix, SourceSet sourceSet) {
        String pp = packagePrefix == null || packagePrefix.isBlank() ? "" : packagePrefix;
        if (sourceSet == null || sourceSet.name() == null || sourceSet.name().isBlank()) return pp;
        String name = sourceSet.name().toLowerCase().replaceAll("[.:-]", "_");
        if (name.endsWith("_jar")) name = name.substring(0, name.length() - 4);
        if (pp.isBlank()) return name;
        return pp + "." + name;
    }

    protected static Map<MethodInfo, Integer> methodCallFrequencies(ParseResult parseResult) throws IOException {
        Map<MethodInfo, Integer> methodHistogram = new HashMap<>();
        parseResult.primaryTypes().stream()
                .flatMap(TypeInfo::recursiveSubTypeStream)
                .flatMap(TypeInfo::constructorAndMethodStream)
                .forEach(mi -> {
                    mi.methodBody().visit(e -> {
                        MethodInfo methodInfo = null;
                        if (e instanceof MethodCall mc &&
                            !parseResult.primaryTypes().contains(mc.methodInfo().typeInfo().primaryType())) {
                            methodInfo = mc.methodInfo();
                        } else if (e instanceof ConstructorCall cc && cc.constructor() != null
                                   && !parseResult.primaryTypes().contains(cc.constructor().typeInfo().primaryType())) {
                            methodInfo = cc.constructor();
                        }
                        if (methodInfo != null) {
                            methodHistogram.merge(methodInfo, 1, Integer::sum);
                        }
                        return true;
                    });
                });
        return methodHistogram;
    }
}
