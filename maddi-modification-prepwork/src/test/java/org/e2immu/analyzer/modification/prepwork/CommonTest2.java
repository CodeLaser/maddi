package org.e2immu.analyzer.modification.prepwork;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.util.internal.graph.G;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;

public class CommonTest2 {
    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected Summary summary;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("io.codelaser.jfocus.refactor")).setLevel(Level.DEBUG);
    }

    protected record R(List<Info> analysisOrder, G<Info> dependencyGraph) {
    }

    protected R init(Map<String, String> sourcesByFqn) throws IOException {
        Map<String, String> sourcesByURIString = sourcesByFqn.entrySet()
                .stream().collect(Collectors.toUnmodifiableMap(
                        e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                // NOTE: No access to ToolChain
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        SourceSet main = new SourceSetImpl("main",

                )
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        JavaInspector.ParseOptions parseOptions = new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setDetailedSources(true).build();
        summary = javaInspector.parse(sourcesByURIString, parseOptions);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        G<Info> graph = prepAnalyzer.doPrimaryTypesReturnGraph(Set.copyOf(summary.types()));
        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        return new R(cao.go(graph), graph);
    }
}
