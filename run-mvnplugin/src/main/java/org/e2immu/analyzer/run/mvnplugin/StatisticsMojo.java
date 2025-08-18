package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Mojo(name = StatisticsMojo.STATISTICS_GOAL, defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class StatisticsMojo extends CommonMojo {
    public static final String STATISTICS_GOAL = "statistics";

    @Parameter(property = "methodCallFrequencies", defaultValue = "${project.build.directory}/methodCallFrequencies.txt")
    private File methodCallFrequencies;

    @Parameter(property = "methodCallFrequenciesMain", defaultValue = "${project.build.directory}/methodCallFrequenciesMain.txt")
    private File methodCallFrequenciesMain;

    @Parameter(property = "methodCallFrequenciesTest", defaultValue = "${project.build.directory}/methodCallFrequenciesTest.txt")
    private File methodCallFrequenciesTest;

    @Parameter(property = "methodCallFrequenciesBySourceSet", defaultValue = "${project.build.directory}/methodCallFrequenciesBySourceSet.txt")
    private File methodCallFrequenciesBySourceSet;

    @Parameter(property = "methodCallFrequenciesMainBySourceSet", defaultValue = "${project.build.directory}/methodCallFrequenciesMainBySourceSet.txt")
    private File methodCallFrequenciesMainBySourceSet;

    @Parameter(property = "methodCallFrequenciesTestBySourceSet", defaultValue = "${project.build.directory}/methodCallFrequenciesTestBySourceSet.txt")
    private File methodCallFrequenciesTestBySourceSet;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            // Create output directory if it doesn't exist
            if (methodCallFrequencies.getParentFile().mkdirs()) {
                getLog().info("Created directories for " + methodCallFrequencies.getAbsolutePath());
            }
            ParseSourcesResult psr = parseSources();
            methodCallFrequencies(psr.parseResult(), methodCallFrequencies, methodCallFrequenciesBySourceSet,
                    t -> true);
            methodCallFrequencies(psr.parseResult(), methodCallFrequenciesMain, methodCallFrequenciesMainBySourceSet,
                    t -> !t.compilationUnit().sourceSet().test());
            methodCallFrequencies(psr.parseResult(), methodCallFrequenciesTest, methodCallFrequenciesTest,
                    t -> t.compilationUnit().sourceSet().test());
        } catch (RuntimeException | IOException | DependencyResolutionException e) {
            throw new MojoExecutionException("Failed to write input configuration", e);
        }
    }

    private void methodCallFrequencies(ParseResult parseResult, File freqFile, File freqFileBySourceSet,
                                       Predicate<TypeInfo> filter) throws IOException {
        Map<String, Integer> methodHistogram = new HashMap<>();
        Map<String, Integer> sourceSetHistogram = new HashMap<>();
        parseResult.primaryTypes().stream()
                .filter(filter)
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
                            methodHistogram.merge(methodInfo.fullyQualifiedName(), 1, Integer::sum);
                            SourceSet sourceSet = methodInfo.typeInfo().compilationUnit().sourceSet();
                            if (sourceSet != null) {
                                sourceSetHistogram.merge(sourceSet.name(), 1, Integer::sum);
                            }
                        }
                        return true;
                    });
                });
        writeHistogram(methodHistogram, freqFile);
        writeHistogram(sourceSetHistogram, freqFileBySourceSet);
    }

    private void writeHistogram(Map<String, Integer> methodHistogram, File freqFile) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(freqFile), StandardCharsets.UTF_8)) {
            methodHistogram.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                    .forEach(e -> {
                        try {
                            osw.append(String.valueOf(e.getValue())).append(" ").append(e.getKey()).append("\n");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }
}
