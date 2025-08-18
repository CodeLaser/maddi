package org.e2immu.analyzer.aapi.parser;


import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.io.DecoratorImpl;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.TypeDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestComposer.class);
    public static final String TEST_DIR = "build/testAAPI";

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);
        List<TypeInfo> primaryTypes = javaInspector.compiledTypesManager()
                .typesLoaded().stream().filter(TypeInfo::isPrimaryType).toList();
        LOGGER.info("Have {} primary types loaded", primaryTypes.size());
        Collection<TypeInfo> apiTypes = composer.compose(primaryTypes);

        Path defaultDestination = Path.of(TEST_DIR);
        defaultDestination.toFile().mkdirs();
        try (Stream<Path> walk = Files.walk(defaultDestination)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }

        Map<Element, Element> dollarMap = composer.translateFromDollarToReal();
        composer.write(apiTypes, TEST_DIR, new DecoratorImpl(javaInspector.runtime(), dollarMap));

        String ju = Files.readString(new File(TEST_DIR, "org/e2immu/testannotatedapi/JavaUtil.java").toPath());
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

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);
        TypeInfo typeDescriptor = javaInspector.compiledTypesManager().getOrLoad(TypeDescriptor.class);
        Collection<TypeInfo> res = composer.compose(Set.of(typeDescriptor));
        assertEquals(1, res.size());
        @Language("java")
        String expected = """
                package org.e2immu.testannotatedapi;
                import java.lang.invoke.TypeDescriptor;
                import java.util.List;
                public class JavaLangInvoke {
                    public static final String PACKAGE_NAME = "java.lang.invoke";
                    //public interface TypeDescriptor
                    class TypeDescriptor$ {
                        //public interface OfField implements TypeDescriptor
                        class OfField<F extends TypeDescriptor.OfField<F>> {
                            boolean isArray() { return false; }
                            boolean isPrimitive() { return false; }
                            F componentType() { return null; }
                            F arrayType() { return null; }
                        }
                
                        //public interface OfMethod implements TypeDescriptor
                        class OfMethod<F extends TypeDescriptor.OfField<F>, M extends TypeDescriptor.OfMethod<F, M>> {
                            int parameterCount() { return 0; }
                            F parameterType(int i) { return null; }
                            F returnType() { return null; }
                            F [] parameterArray() { return null; }
                            List<F> parameterList() { return null; }
                            M changeReturnType(F f) { return null; }
                            M changeParameterType(int i, F f) { return null; }
                            M dropParameterTypes(int i, int i1) { return null; }
                            M insertParameterTypes(int i, F ... f) { return null; }
                        }
                        String descriptorString() { return null; }
                    }
                }
                """;
        assertEquals(expected, javaInspector.print2(res.stream().findFirst().orElseThrow()));
    }


    @DisplayName("double printing of type parameters, part 2")
    @Test
    public void test3() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);

        TypeInfo arrays = javaInspector.compiledTypesManager().getOrLoad(Arrays.class);
        MethodInfo parallelSort = arrays.methodStream()
                .filter(mi -> "parallelSort".equals(mi.name())
                              && mi.parameters().size() == 3
                              && mi.parameters().getFirst().parameterizedType().isTypeParameter()).findFirst().orElseThrow();
        assertEquals("java.util.Arrays.parallelSort(T extends Comparable<? super T>[],int,int)",
                parallelSort.fullyQualifiedName());

        Collection<TypeInfo> res = composer.compose(Set.of(arrays));
        assertEquals(1, res.size());

        TypeInfo typeInfo = res.stream().findFirst().orElseThrow();
        String printed = javaInspector.print2(typeInfo);

        TypeInfo arraysDollar = typeInfo.findSubType("Arrays$");
        MethodInfo parallelSortCopy = arraysDollar.methodStream()
                .filter(mi -> "parallelSort".equals(mi.name())
                              && mi.parameters().size() == 3
                              && mi.parameters().getFirst().parameterizedType().isTypeParameter()).findFirst().orElseThrow();

        assertEquals("""
                org.e2immu.testannotatedapi.JavaUtil.Arrays$.parallelSort(T extends Comparable<? super T>[],int,int)\
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
        javaInspector.initialize(inputConfigurationBuilder.build());

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);

        TypeInfo commandLine = javaInspector.compiledTypesManager().getOrLoad("picocli.CommandLine", null);
        MethodInfo call = commandLine.findUniqueMethod("call", 2);
        assertEquals("picocli.CommandLine.call(C extends java.util.concurrent.Callable<T>,String...)",
                call.fullyQualifiedName());

        Collection<TypeInfo> res = composer.compose(Set.of(commandLine));
        assertEquals(1, res.size());

        TypeInfo typeInfo = res.stream().findFirst().orElseThrow();
        String printed = javaInspector.print2(typeInfo);

        TypeInfo commandLineDollar = typeInfo.findSubType("CommandLine$");
        MethodInfo callCopy = commandLineDollar.findUniqueMethod("call", 2);

        assertEquals("""
                org.e2immu.testannotatedapi.Picocli.CommandLine$.call(C extends java.util.concurrent.Callable<T>,String...)\
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

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);
        TypeInfo annotationConsumer = javaInspector.compiledTypesManager()
                .getOrLoad("org.junit.jupiter.params.support.AnnotationConsumer", null);
        Collection<TypeInfo> res = composer.compose(Set.of(annotationConsumer));
        assertEquals(1, res.size());

        @Language("java")
        String expected = """
                package org.e2immu.testannotatedapi;
                import java.lang.annotation.Annotation;
                public class OrgJunitJupiterParamsSupport {
                    public static final String PACKAGE_NAME = "org.junit.jupiter.params.support";
                    //public interface AnnotationConsumer implements Consumer<A>
                    class AnnotationConsumer$<A extends Annotation> { }
                }
                """;
        TypeInfo typeInfo = res.stream().findFirst().orElseThrow();
        assertEquals(expected, javaInspector.print2(typeInfo));
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

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(
                "org.springframework.security.config.annotation.web.configurers.AbstractInterceptUrlConfigurer",
                null);
        assertNotNull(typeInfo);
        Collection<TypeInfo> res = composer.compose(Set.of(typeInfo));
        assertEquals(1, res.size());

        /*
        Class AbstractInterceptUrlConfigurer.AbstractInterceptUrlRegistry<R extends AbstractInterceptUrlConfigurer<C,H>.AbstractInterceptUrlRegistry<R,T>,T>
         */
        @Language("java")
        String expected = """
                package org.e2immu.testannotatedapi;
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
        assertEquals(expected, javaInspector.print2(newType));
    }


    @DisplayName("spacing problem")
    @Test
    public void test7() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/springframework/security/config")
                .addClassPath(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/springframework/security/web")
                .addClassPath("jmod:java.base");

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfigurationBuilder.build());

        Composer composer = new Composer(javaInspector, set -> "org.e2immu.testannotatedapi", w -> true);
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(
                "org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer",
                null);
        assertNotNull(typeInfo);

        Collection<TypeInfo> res = composer.compose(Set.of(typeInfo));
        assertEquals(1, res.size());

        @Language("java")
        String expected = """
                package org.e2immu.testannotatedapi;
                import org.springframework.security.config.annotation.ObjectPostProcessor;
                import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
                import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
                public class OrgSpringframeworkSecurityConfigAnnotationWebConfigurers {
                    public static final String PACKAGE_NAME = "org.springframework.security.config.annotation.web.configurers";
                    //public final class AuthorizeHttpRequestsConfigurer extends AbstractHttpConfigurer<AuthorizeHttpRequestsConfigurer<H>,H>
                    class AuthorizeHttpRequestsConfigurer$<H extends HttpSecurityBuilder<H>> {
                        //public final class AuthorizationManagerRequestMatcherRegistry extends AbstractRequestMatcherRegistry<AuthorizeHttpRequestsConfigurer<H>.AuthorizedUrl>
                        class AuthorizationManagerRequestMatcherRegistry {
                            AuthorizeHttpRequestsConfigurer<H> . AuthorizationManagerRequestMatcherRegistry withObjectPostProcessor(
                                ObjectPostProcessor<?> objectPostProcessor) { return null; }
                
                            AuthorizeHttpRequestsConfigurer<H> . AuthorizationManagerRequestMatcherRegistry shouldFilterAllDispatcherTypes(
                                boolean shouldFilter) { return null; }
                            H and() { return null; }
                        }
                
                        //public class AuthorizedUrl
                        class AuthorizedUrl {
                            //public final class AuthorizedUrlVariable
                            class AuthorizedUrlVariable { }
                
                            //frequency 1
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizedUrl not() { return null; }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry permitAll() {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry denyAll() {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry hasRole(String role) {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry hasAnyRole(
                                String ... roles) { return null; }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry hasAuthority(
                                String authority) { return null; }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry hasAnyAuthority(
                                String ... authorities) { return null; }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry authenticated() {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry fullyAuthenticated() {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry rememberMe() {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> . AuthorizationManagerRequestMatcherRegistry anonymous() {
                                return null;
                            }
                
                            AuthorizeHttpRequestsConfigurer<Object> .AuthorizedUrl. AuthorizedUrlVariable hasVariable(String variable) {
                                return null;
                            }
                        }
                        AuthorizeHttpRequestsConfigurer<H> . AuthorizationManagerRequestMatcherRegistry getRegistry() { return null; }
                        void configure(H http) { }
                    }
                }
                """;
        TypeInfo newType = res.stream().findFirst().orElseThrow();
        TypeInfo configurer = newType.findSubType("AuthorizeHttpRequestsConfigurer$");
        TypeInfo authorizedUrl = configurer.findSubType("AuthorizedUrl");
        MethodInfo not = authorizedUrl.findUniqueMethod("not", 0);
        Map<MethodInfo, Integer> freq = Map.of(not, 1);
        assertEquals(expected, javaInspector.print2(newType, new DecoratorWithComments(javaInspector.runtime(), freq),
                javaInspector.importComputer(4)));
    }


    static class DecoratorWithComments extends DecoratorImpl {
        private final Map<MethodInfo, Integer> methodCallFrequencies;
        private final org.e2immu.language.cst.api.runtime.Runtime runtime;

        public DecoratorWithComments(Runtime runtime, Map<MethodInfo, Integer> methodCallFrequencies) {
            super(runtime, Map.of());
            this.runtime = runtime;
            this.methodCallFrequencies = methodCallFrequencies;
        }

        @Override
        public List<Comment> comments(Element info) {
            List<Comment> comments = super.comments(info);
            Integer frequency = info instanceof MethodInfo mi ? methodCallFrequencies.get(mi) : null;
            if (frequency != null) {
                Comment comment = runtime.newSingleLineComment(null, "frequency " + frequency);
                return Stream.concat(Stream.of(comment), comments.stream()).toList();
            }
            return comments;
        }
    }
}
