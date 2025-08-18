package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.util.internal.graph.G;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {
    private static CompiledTypesManager compiledTypesManager;

    static List<TypeInfo> allTypes;
    static List<TypeInfo> sorted;
    static G<TypeInfo> graph;
    private static Runtime runtime;

    public static CompiledTypesManager compiledTypesManager() {
        return compiledTypesManager;
    }

    public static Runtime runtime() {
        return runtime;
        
    }
    @BeforeAll
    public static void beforeAll() throws IOException {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.DEBUG);

        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        annotatedApiParser.initialize(null,
                List.of(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/slf4j",
                        JavaInspectorImpl.E2IMMU_SUPPORT,
                        JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api",
                        JAR_WITH_PATH_PREFIX + "org/opentest4j",
                        "jmod:java.datatransfer",
                        "jmod:java.desktop"),
                List.of("../e2immu-aapi-archive/src/main/java/org/e2immu/analyzer/aapi/archive/v2"),
                List.of());
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(annotatedApiParser.runtime(), annotatedApiParser,
                true);
       ShallowAnalyzer.Result sr = shallowAnalyzer.go(annotatedApiParser.types());

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(annotatedApiParser.runtime());
        prepAnalyzer.initialize(annotatedApiParser.javaInspector().compiledTypesManager().typesLoaded());

        sorted = sr.sorted();
        graph = sr.typeGraph();
        allTypes = sr.allTypes();
        compiledTypesManager = annotatedApiParser.javaInspector().compiledTypesManager();
        runtime = annotatedApiParser.runtime();
    }

    protected void testImmutableContainer(TypeInfo typeInfo, boolean hcImmutable, boolean hcIndependent) {
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE);
        Value.Immutable expectImmutable = hcImmutable
                ? ValueImpl.ImmutableImpl.IMMUTABLE_HC : ValueImpl.ImmutableImpl.IMMUTABLE;
        assertSame(expectImmutable, immutable);

        Value.Independent independent = typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        Value.Independent expectIndependent = hcIndependent
                ? ValueImpl.IndependentImpl.INDEPENDENT_HC : ValueImpl.IndependentImpl.INDEPENDENT;
        assertSame(expectIndependent, independent);

        boolean container = typeInfo.analysis().getOrDefault(CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE).isTrue();
        assertTrue(container);
    }
}
