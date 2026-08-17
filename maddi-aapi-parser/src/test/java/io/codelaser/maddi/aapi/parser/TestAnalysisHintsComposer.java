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

package io.codelaser.maddi.aapi.parser;


import ch.qos.logback.classic.Level;
import io.codelaser.maddi.modification.prepwork.io.DecoratorImpl;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.TypeDescriptor;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnalysisHintsComposer extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAnalysisHintsComposer.class);
    public static final String TEST_DIR = "build/testAAPI";

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.aapi")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException, URISyntaxException {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);

        JavaInspector javaInspector = new io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl();
        javaInspector.preload("java.base::java.util");
        javaInspector.preload("org.slf4j");
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .addClassPathParts(javaBase, maddiSupport, slf4jApi)
                .build();
        javaInspector.preload("io.codelaser.maddi.annotation.");

        javaInspector.initialize(inputConfiguration);
        javaInspector.onlyPreload();
        AnalysisHintsComposer analysisHintsComposer = new AnalysisHintsComposer(javaInspector,
                _ -> "io.codelaser.maddi.testannotatedapi", _ -> true);
        List<TypeInfo> primaryTypes = javaInspector.compiledTypesManager()
                .typesLoaded(true).stream().filter(TypeInfo::isPrimaryType).toList();
        LOGGER.info("Have {} primary types loaded", primaryTypes.size());
        Collection<TypeInfo> apiTypes = analysisHintsComposer.compose(primaryTypes);

        Path defaultDestination = Path.of(TEST_DIR);
        defaultDestination.toFile().mkdirs();
        try (Stream<Path> walk = Files.walk(defaultDestination)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }

        Map<Element, Element> dollarMap = analysisHintsComposer.translateFromDollarToReal();
        analysisHintsComposer.write(apiTypes, TEST_DIR, new DecoratorImpl(javaInspector.runtime(),
                javaInspector.mainSources(), dollarMap));

        String ju = Files.readString(new File(TEST_DIR, "io/codelaser/maddi/testannotatedapi/JavaUtil.java").toPath());
        assertTrue(ju.contains("//public abstract class AbstractSet extends AbstractCollection<E> implements Set<E>"));
    }

    @DisplayName("double printing of type parameters")
    @Test
    public void test2() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());

        AnalysisHintsComposer analysisHintsComposer = new AnalysisHintsComposer(javaInspector, set -> "io.codelaser.maddi.testannotatedapi", w -> true);
        TypeInfo typeDescriptor = javaInspector.compiledTypesManager().type(TypeDescriptor.class);
        Collection<TypeInfo> res = analysisHintsComposer.compose(Set.of(typeDescriptor));
        assertEquals(1, res.size());
        @Language("java")
        String expected = """
                package io.codelaser.maddi.testannotatedapi;
                import java.lang.invoke.TypeDescriptor;
                import java.util.List;
                public class JavaLangInvoke {
                    public static final String PACKAGE_NAME = "java.lang.invoke";
                    //public interface TypeDescriptor
                    class TypeDescriptor$ {
                        //interface OfField implements TypeDescriptor
                        class OfField<F extends TypeDescriptor.OfField<F>> {
                            F arrayType() { return null; }
                            F componentType() { return null; }
                            boolean isArray() { return false; }
                            boolean isPrimitive() { return false; }
                        }
                
                        //interface OfMethod implements TypeDescriptor
                        class OfMethod<F extends TypeDescriptor.OfField<F>, M extends TypeDescriptor.OfMethod<F, M>> {
                            M changeParameterType(int i, F f) { return null; }
                            M changeReturnType(F f) { return null; }
                            M dropParameterTypes(int i, int i1) { return null; }
                            M insertParameterTypes(int i, F ... f) { return null; }
                            F [] parameterArray() { return null; }
                            int parameterCount() { return 0; }
                            List<F> parameterList() { return null; }
                            F parameterType(int i) { return null; }
                            F returnType() { return null; }
                        }
                        String descriptorString() { return null; }
                    }
                }
                """;
        assertEquals(expected, javaInspector.print2(res.stream().findFirst().orElseThrow().compilationUnit()));
    }


    @DisplayName("double printing of type parameters, part 2")
    @Test
    public void test3() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());
        javaInspector.javaBase().computePriorityDependencies();
        AnalysisHintsComposer analysisHintsComposer = new AnalysisHintsComposer(javaInspector, set -> "io.codelaser.maddi.testannotatedapi", w -> true);

        TypeInfo arrays = javaInspector.compiledTypesManager().type(Arrays.class);
        MethodInfo parallelSort = arrays.methodStream()
                .filter(mi -> "parallelSort".equals(mi.name())
                              && mi.parameters().size() == 3
                              && mi.parameters().getFirst().parameterizedType().isTypeParameter()).findFirst().orElseThrow();
        assertEquals("java.util.Arrays.parallelSort(Comparable[],int,int)",
                parallelSort.fullyQualifiedName());

        Collection<TypeInfo> res = analysisHintsComposer.compose(Set.of(arrays));
        assertEquals(1, res.size());

        TypeInfo typeInfo = res.stream().findFirst().orElseThrow();
        String printed = javaInspector.print2(typeInfo.compilationUnit());

        TypeInfo arraysDollar = typeInfo.findSubType("Arrays$");
        MethodInfo parallelSortCopy = arraysDollar.methodStream()
                .filter(mi -> "parallelSort".equals(mi.name())
                              && mi.parameters().size() == 3
                              && mi.parameters().getFirst().parameterizedType().isTypeParameter()).findFirst().orElseThrow();

        assertEquals("""
                io.codelaser.maddi.testannotatedapi.JavaUtil.Arrays$.parallelSort(Comparable[],int,int)\
                """, parallelSortCopy.fullyQualifiedName());

        TypeParameter tp0 = parallelSortCopy.typeParameters().getFirst();

        ParameterizedType pt0 = parallelSortCopy.parameters().getFirst().parameterizedType();
        assertSame(tp0, pt0.typeParameter());

        ParameterizedType tb0 = tp0.typeBounds().getFirst();
        assertEquals("java.lang.Comparable", tb0.typeInfo().fullyQualifiedName());
        ParameterizedType tb0p0 = tb0.parameters().getFirst();
        assertSame(tp0, tb0p0.typeParameter());

        // double printing...
        assertFalse(printed.contains("<T extends Comparable<? super T extends Comparable<? super T>>>"));
    }


    @DisplayName("each type parameter doubled")
    @Test
    public void test4() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "picocli")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = inputConfigurationBuilder.build();
        javaInspector.initialize(inputConfiguration);
        inputConfiguration.classPathParts().forEach(SourceSet::computePriorityDependencies);

        AnalysisHintsComposer analysisHintsComposer = new AnalysisHintsComposer(javaInspector, set -> "io.codelaser.maddi.testannotatedapi", w -> true);

        TypeInfo commandLine = javaInspector.compiledTypesManager().type("picocli.CommandLine",
                javaInspector.mainSources());
        MethodInfo call = commandLine.findUniqueMethod("call", 2);
        assertEquals("picocli.CommandLine.call(java.util.concurrent.Callable,String[])",
                call.fullyQualifiedName());

        Collection<TypeInfo> res = analysisHintsComposer.compose(Set.of(commandLine));
        assertEquals(1, res.size());

        TypeInfo typeInfo = res.stream().findFirst().orElseThrow();
        String printed = javaInspector.print2(typeInfo.compilationUnit());

        TypeInfo commandLineDollar = typeInfo.findSubType("CommandLine$");
        MethodInfo callCopy = commandLineDollar.findUniqueMethod("call", 2);

        assertEquals("""
                io.codelaser.maddi.testannotatedapi.Picocli.CommandLine$.call(java.util.concurrent.Callable,String[])\
                """, callCopy.fullyQualifiedName());
        assertEquals(2, callCopy.typeParameters().size());
        TypeParameter tp0 = callCopy.typeParameters().getFirst();
        assertEquals("C=TP#0 in CommandLine$.call", tp0.toString());
        TypeParameter tp1 = callCopy.typeParameters().get(1);
        assertEquals("T=TP#1 in CommandLine$.call", tp1.toString());

        ParameterizedType pt0 = callCopy.parameters().getFirst().parameterizedType();
        assertSame(tp0, pt0.typeParameter());

        ParameterizedType tb0 = tp0.typeBounds().getFirst();
        assertEquals("java.util.concurrent.Callable", tb0.typeInfo().fullyQualifiedName());
        ParameterizedType tb0p0 = tb0.parameters().getFirst();
        assertSame(tp1, tb0p0.typeParameter());

        // double printing...
        assertFalse(printed.contains("<T extends Comparable<? super T extends Comparable<? super T>>>"));
    }


    @DisplayName("type referenced in annotation")
    @Test
    public void test5() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/junit/jupiter/params")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());

        AnalysisHintsComposer analysisHintsComposer = new AnalysisHintsComposer(javaInspector, set -> "io.codelaser.maddi.testannotatedapi", w -> true);
        TypeInfo annotationConsumer = javaInspector.compiledTypesManager()
                .type("org.junit.jupiter.params.support.AnnotationConsumer",
                        javaInspector.mainSources());
        Collection<TypeInfo> res = analysisHintsComposer.compose(Set.of(annotationConsumer));
        assertEquals(1, res.size());

        @Language("java")
        String expected = """
                package io.codelaser.maddi.testannotatedapi;
                import java.lang.annotation.Annotation;
                public class OrgJunitJupiterParamsSupport {
                    public static final String PACKAGE_NAME = "org.junit.jupiter.params.support";
                    //public interface AnnotationConsumer implements Consumer<A>
                    class AnnotationConsumer$<A extends Annotation> { }
                }
                """;
        TypeInfo typeInfo = res.stream().findFirst().orElseThrow();
        assertEquals(expected, javaInspector.print2(typeInfo.compilationUnit()));
    }

    @Test
    public void test6() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/springframework/security/config")
                .addClassPath(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/springframework/security/web")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());

        AnalysisHintsComposer analysisHintsComposer = new AnalysisHintsComposer(javaInspector, set -> "io.codelaser.maddi.testannotatedapi", w -> true);
        TypeInfo typeInfo = javaInspector.compiledTypesManager().type(
                "org.springframework.security.config.annotation.web.configurers.AbstractInterceptUrlConfigurer",
                javaInspector.mainSources());
        assertNotNull(typeInfo);
        Collection<TypeInfo> res = analysisHintsComposer.compose(Set.of(typeInfo));
        assertEquals(1, res.size());

        /*
        Class AbstractInterceptUrlConfigurer.AbstractInterceptUrlRegistry<R extends AbstractInterceptUrlConfigurer<C,H>.AbstractInterceptUrlRegistry<R,T>,T>
         */
        @Language("java")
        String expected = """
                package io.codelaser.maddi.testannotatedapi;
                import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
                import org.springframework.security.config.annotation.web.configurers.AbstractInterceptUrlConfigurer;
                public class OrgSpringframeworkSecurityConfigAnnotationWebConfigurers {
                    public static final String PACKAGE_NAME = "org.springframework.security.config.annotation.web.configurers";
                    //public abstract class AbstractInterceptUrlConfigurer extends AbstractHttpConfigurer<C,H>
                    class AbstractInterceptUrlConfigurer$<
                        C extends AbstractInterceptUrlConfigurer<C, H>,
                        H extends HttpSecurityBuilder<H>> {
                        //public abstract class AbstractInterceptUrlRegistry extends AbstractConfigAttributeRequestMatcherRegistry<T>
                        class AbstractInterceptUrlRegistry<
                            R extends AbstractInterceptUrlConfigurer<C, H> . AbstractInterceptUrlRegistry<R, T> ,
                            T> {
                            R filterSecurityInterceptorOncePerRequest(boolean filterSecurityInterceptorOncePerRequest) { return null; }
                        }
                        void configure(H http) { }
                    }
                }
                """;
        TypeInfo newType = res.stream().findFirst().orElseThrow();
        assertEquals(expected, javaInspector.print2(newType.compilationUnit()));
    }
}
