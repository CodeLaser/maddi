package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.e2immu.analyzer.run.config.util.ComputeDependencies;
import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
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

    @Parameter(property = "jmods", defaultValue = "java.base")
    private String jmods;

    @Parameter(property = "testSourcePackages", defaultValue = "")
    private String testSourcePackages;

    @Parameter(property = "sourcePackages", defaultValue = "")
    private String sourcePackages;

    @Parameter(property = "sourceEncoding", defaultValue = "UTF-8")
    private String sourceEncoding;

    @Component
    private ProjectDependenciesResolver dependenciesResolver;

    protected InputConfiguration makeInputConfiguration() throws DependencyResolutionException {
        InputConfiguration.Builder builder = new InputConfigurationImpl.Builder();
        builder.setAlternativeJREDirectory(jre);
        builder.setWorkingDirectory(workingDirectory);
        File absWorkingDirectory = workingDirectory == null || workingDirectory.isBlank()
                ? project.getBasedir().getAbsoluteFile() : new File(workingDirectory).getAbsoluteFile();

        Set<String> excludeFromClasspathSet = excludeFromClasspath == null || excludeFromClasspath.isBlank() ? Set.of() :
                Arrays.stream(excludeFromClasspath.split("[;,]\\s*")).collect(Collectors.toUnmodifiableSet());
        ComputeDependencies.SourceSetDependencies result = new ComputeSourceSets(absWorkingDirectory,
                dependenciesResolver, project,
                session, getLog()).compute(sourceEncoding, sourcePackages, testSourcePackages, excludeFromClasspathSet);

        makeJavaModules(jmods).forEach(set -> result.sourceSetsByName().put(set.name(), set));

        G<String> graph = new ComputeDependencies(s ->getLog().debug(s)).go(result);
        List<String> linearization = Linearize.linearize(graph).asList(String::compareToIgnoreCase);
        if(getLog().isDebugEnabled()) {
            getLog().debug("Graph: " + graph);
            getLog().debug("Linearization:\n  " + String.join("\n  ", linearization) + "\n");
        }
        for (String name : linearization) {
            Map<V<String>, Long> edges = graph.edges(new V<>(name));
            Set<SourceSet> dependencies = edges == null ? Set.of() : edges.keySet()
                    .stream().map(v -> result.sourceSetsByName().get(v.t()))
                    .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
            SourceSet sourceSet = result.sourceSetsByName().get(name);
            if (sourceSet == null) {
                getLog().warn("Don't know source set " + name);
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

    protected record ParseSourcesResult(ParseResult parseResult,
                                        JavaInspector javaInspector,
                                        InputConfiguration inputConfiguration) {
    }

    protected ParseSourcesResult parseSources() throws DependencyResolutionException, IOException {

        InputConfiguration inputConfiguration = makeInputConfiguration();
        JavaInspector javaInspector = new JavaInspectorImpl();

        InputConfiguration withJavaModules = inputConfiguration.withE2ImmuSupportFromClasspath().withDefaultModules();
        getLog().info("Working directory: " + withJavaModules.workingDirectory());
        javaInspector.initialize(withJavaModules);

        String jdkSpec = ToolChain.extractLibraryName(javaInspector.compiledTypesManager().typesLoaded(),
                false);
        String mapped = ToolChain.mapJreShortNameToAnalyzedPackageShortName(jdkSpec);
        getLog().info("Resolved analyzed package files for " + jdkSpec + " -> " + mapped);

        JavaInspector.ParseOptions parseOptions = new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setDetailedSources(true).build();
        ParseResult parseResult = javaInspector.parse(parseOptions).parseResult();
        return new ParseSourcesResult(parseResult, javaInspector, inputConfiguration);
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
