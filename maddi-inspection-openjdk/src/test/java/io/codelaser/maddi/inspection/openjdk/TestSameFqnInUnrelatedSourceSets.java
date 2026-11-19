/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.inspection.openjdk;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * ONE NAME DECLARED BY SOURCE SETS THAT DO NOT DEPEND ON EACH OTHER. javac compiles each set on its own class path,
 * so each use resolves to the copy that set can see; maddi's registry ({@code InfoByFqn}) holds one entry per name
 * and answered a lookup with whatever copy was registered, reachable or not.
 * <p>
 * Found on quarkus c64e3b71dc43 (whole Maven reactor, 1,677 source sets), six non-tolerable parse failures:
 * <ul>
 *     <li>several integration-tests generate the same gRPC classes ({@code examples.MutinyGreeterGrpc}, ...) into
 *     their own {@code target/generated-sources/grpc}. {@code reactive-messaging-pulsar} has no proto of its own: it
 *     gets them from its dependency {@code grpc-proto4}, compiled. Its {@code GpServiceImpl} resolved to
 *     {@code oidc-wiremock}'s source copy, and the class file arriving later was refused ("already committed, and a
 *     second definition of it is now arriving FROM A COMPILED ARTIFACT with a member the committed one does not
 *     have"). {@link #aCompiledDependencyIsNotAnUnrelatedSetsSource}.</li>
 *     <li>{@code qute/core}'s {@code ParserTest} declares a local {@code class Dummy} in a test method, and
 *     {@code arc/tcks/cdi-tck-signature} declares a top-level {@code Dummy} in the default package. javac names a
 *     local class by its simple name, so the lookup found the other set's {@code Dummy} ("a type of a file this set
 *     is parsing resolved to ...cdi-tck-signature...'s copy: Dummy"). {@link #aLocalClassIsNotAnUnrelatedSetsTopLevelType}.
 *     </li>
 * </ul>
 * The unrelated set is declared, and therefore scanned, first in both.
 */
public class TestSameFqnInUnrelatedSourceSets {

    private static final String CONSUMER = TEST_PROTOCOL;
    private static final String UNRELATED = TEST_PROTOCOL + "W";

    /** oidc-wiremock's copy: generated from another proto, without the method the consumer calls. */
    @Language("java")
    private static final String GREETER_UNRELATED = """
            package examples;
            public class Greeter {
                public String hello() { return "w"; }
            }
            """;

    /** grpc-proto4's copy, which the consumer has as a compiled dependency. */
    @Language("java")
    private static final String GREETER_COMPILED = """
            package examples;
            public class Greeter {
                public String hello() { return "p"; }
                public String greet(String name) { return name; }
            }
            """;

    @Language("java")
    private static final String USE_GREETER = """
            package use;
            import examples.Greeter;
            public class Use extends Greeter {
                public String go() { return greet("x"); }
            }
            """;

    @DisplayName("a type the consumer has compiled resolves to that class file, not to an unrelated set's source")
    @Test
    public void aCompiledDependencyIsNotAnUnrelatedSetsSource() throws Exception {
        Path tmp = Files.createTempDirectory("same-fqn-");
        try {
            Path protoClasses = compile(tmp, "proto4", Map.of("examples.Greeter", GREETER_COMPILED));
            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet protoLib = new SourceSetImpl.Builder()
                    .setName("proto4-classes").setUri(protoClasses.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            SourceSet unrelated = new SourceSetImpl.Builder().setName(UNRELATED)
                    .setUri(URI.create("file:/oidc-wiremock/")).setDependencies(List.of(javaBase)).build();
            SourceSet consumer = new SourceSetImpl.Builder().setName(CONSUMER)
                    .setUri(URI.create("file:/pulsar/")).setDependencies(List.of(javaBase, protoLib)).build();
            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(unrelated, Map.of("examples.Greeter", GREETER_UNRELATED));
            sources.put(consumer, Map.of("use.Use", USE_GREETER));
            Summary summary = parse(List.of(javaBase, protoLib), sources);

            assertFalse(summary.haveErrors(), "the parse succeeds: " + summary.parseExceptions());
            TypeInfo use = primary(summary, "use.Use", CONSUMER);
            TypeInfo greeter = use.parentClass().typeInfo();
            assertEquals("proto4-classes", sourceSet(greeter),
                    "Use extends the Greeter of its own class path, not oidc-wiremock's");
            MethodInfo go = use.methods().stream().filter(m -> "go".equals(m.name())).findFirst().orElseThrow();
            assertNotNull(go.methodBody(), "go() is parsed");
        } finally {
            delete(tmp);
        }
    }

    /** cdi-tck-signature's Dummy.java, in the default package. */
    @Language("java")
    private static final String DUMMY = """
            public class Dummy {
            }
            """;

    /** qute's ParserTest, reduced: a local class Dummy, used in the same method. */
    @Language("java")
    private static final String PARSER_TEST = """
            package q;
            public class ParserTest {
                interface Init { String origin(); }
                static String split(Init init) { return init.origin(); }
                public String test() {
                    class Dummy implements Init {
                        public String origin() { return "d"; }
                    }
                    return split(new Dummy());
                }
            }
            """;

    @DisplayName("a local class resolves to itself, not to an unrelated set's top-level type of the same name")
    @Test
    public void aLocalClassIsNotAnUnrelatedSetsTopLevelType() throws Exception {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet unrelated = new SourceSetImpl.Builder().setName(UNRELATED)
                .setUri(URI.create("file:/cdi-tck-signature/")).setDependencies(List.of(javaBase)).build();
        SourceSet consumer = new SourceSetImpl.Builder().setName(CONSUMER)
                .setUri(URI.create("file:/qute/")).setDependencies(List.of(javaBase)).build();
        Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
        sources.put(unrelated, Map.of("Dummy", DUMMY));
        sources.put(consumer, Map.of("q.ParserTest", PARSER_TEST));
        Summary summary = parse(List.of(javaBase), sources);

        assertFalse(summary.haveErrors(), "the parse succeeds: " + summary.parseExceptions());
        TypeInfo parserTest = primary(summary, "q.ParserTest", CONSUMER);
        assertNotNull(parserTest.methods().stream().filter(m -> "test".equals(m.name())).findFirst().orElseThrow()
                .methodBody(), "test() is parsed");
        assertEquals(UNRELATED, sourceSet(primary(summary, "Dummy", UNRELATED)));
    }

    @Language("java")
    private static final String BOX = """
            package lib;
            public class Box {
                public String get() { return "b"; }
            }
            """;

    @Language("java")
    private static final String USE_BOX = """
            package use;
            import lib.Box;
            public class UseBox extends Box {
                public String go() { return get(); }
            }
            """;

    /*
     CodeLaser/maddi-mod#151, guava's configuration reduced: the sources are the set 'guava/9' (URI
     target/classes/META-INF/versions/9, the multi-release set), the class directory target/classes is the external
     library 'guava/main', and the test sets depend on 'guava/main' only. They see the class files of exactly those
     sources, so a use resolves to the source type, not to a second copy loaded from the class file.
     */
    @DisplayName("a set that depends on another set's class output reaches that set's source types")
    @Test
    public void aDependencyOnTheClassOutputReachesItsSources() throws Exception {
        Path tmp = Files.createTempDirectory("same-fqn-");
        try {
            Path classes = compile(tmp, "guava", Map.of("lib.Box", BOX));
            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet mainClasses = new SourceSetImpl.Builder()
                    .setName("guava/main").setUri(classes.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            SourceSet sources9 = new SourceSetImpl.Builder().setName(UNRELATED)
                    .setUri(classes.resolve("META-INF/versions/9").toUri())
                    .setDependencies(List.of(javaBase, mainClasses)).build();
            SourceSet consumer = new SourceSetImpl.Builder().setName(CONSUMER)
                    .setUri(URI.create("file:/guava-tests/")).setDependencies(List.of(javaBase, mainClasses)).build();
            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(sources9, Map.of("lib.Box", BOX));
            sources.put(consumer, Map.of("use.UseBox", USE_BOX));
            Summary summary = parse(List.of(javaBase, mainClasses), sources);

            assertFalse(summary.haveErrors(), "the parse succeeds: " + summary.parseExceptions());
            TypeInfo useBox = primary(summary, "use.UseBox", CONSUMER);
            TypeInfo box = useBox.parentClass().typeInfo();
            assertEquals(UNRELATED, sourceSet(box), "UseBox extends the source Box, not a copy from the class file");
            assertSame(primary(summary, "lib.Box", UNRELATED), box);
        } finally {
            delete(tmp);
        }
    }

    private static Summary parse(List<SourceSet> classPath, Map<SourceSet, Map<String, String>> sources)
            throws Exception {
        InputConfiguration ic = new InputConfigurationImpl.Builder()
                .addClassPathParts(classPath.toArray(SourceSet[]::new))
                .addSourceSets(sources.keySet().toArray(SourceSet[]::new))
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);
        return javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
    }

    private static TypeInfo primary(Summary summary, String fqn, String sourceSet) {
        return summary.parseResult().primaryTypes().stream()
                .filter(t -> fqn.equals(t.fullyQualifiedName()) && sourceSet.equals(sourceSet(t)))
                .findFirst().orElseThrow(() -> new AssertionError("no " + fqn + " in " + sourceSet));
    }

    private static String sourceSet(TypeInfo type) {
        SourceSet sourceSet = type.primaryType().compilationUnit().sourceSet();
        return sourceSet == null ? "(no source set)" : sourceSet.name();
    }

    private static Path compile(Path tmp, String name, Map<String, String> sources) throws IOException {
        Path src = Files.createDirectories(tmp.resolve(name + "-src"));
        Path classes = Files.createDirectories(tmp.resolve(name + "-classes"));
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path javaFile = src.resolve(e.getKey().replace('.', '/') + ".java");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, e.getValue());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        List<String> args = new ArrayList<>(List.of("-d", classes.toString()));
        try (Stream<Path> walk = Files.walk(src)) {
            walk.filter(p -> p.toString().endsWith(".java")).map(Path::toString).forEach(args::add);
        }
        assertEquals(0, compiler.run(null, null, null, args.toArray(String[]::new)), "fixture compiles: " + name);
        return classes;
    }

    private static void delete(Path tmp) throws IOException {
        try (Stream<Path> walk = Files.walk(tmp)) {
            walk.sorted((x, y) -> y.getNameCount() - x.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
