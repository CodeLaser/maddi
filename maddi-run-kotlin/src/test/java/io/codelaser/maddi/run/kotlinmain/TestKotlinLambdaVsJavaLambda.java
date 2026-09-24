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
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⭐ Tier 3, item 2: <b>the link engine had zero Kotlin tests</b>, and one concrete reason to expect a
 * divergence. {@code VirtualFieldComputer.compute} returns {@code NONE_NONE} — no virtual fields — for any
 * type in package {@code java.util.function}. A Java lambda is a {@code java.util.function.Consumer}; a
 * KOTLIN lambda is a {@code kotlin.jvm.functions.Function1}, which that test does not match, so the two
 * take DIFFERENT paths through the machinery that decides modification and independence.
 *
 * <p>Whether a different path is a different ANSWER is the only question that matters, and it is the one
 * nothing asked. Two shapes, each written twice:
 * <ul>
 *   <li><b>samConverted</b> — a lambda handed to a Java functional interface. The common mixed-language
 *       case: Kotlin SAM-converts, so both sides should meet at {@code s.StringSink}.</li>
 *   <li><b>higherOrder</b> — a callback typed by each language's OWN function type: Kotlin
 *       {@code (String) -> Unit} against Java {@code Consumer<String>}. This is the one that actually
 *       crosses the exclusion above.</li>
 * </ul>
 *
 * <p>⭐ Measured 2026-09-23, all three rows agree on every sensor: the different path is not a different answer
 * for these shapes. {@code higherOrder} sees {@code b} modified through a Kotlin {@code (String) -> Unit} exactly
 * as through a Java {@code Consumer}. Guarded by a zero-placeholder census, so the agreement is not vacuous.
 *
 * <p>2026-09-24: two <b>local functions</b>, which maddi lowers to a local of a {@code FunctionN} type whose value is
 * an anonymous implementation, against the Java lambda a human writes instead. The captured {@code b} is modified
 * through one and only read through the other, on both sides. They live here rather than in
 * {@code TestLoweredShapesVsJava} because {@code FunctionN} is a stdlib type: without the jar the local stays a
 * placeholder.
 *
 * <p>{@code invokesValue} hands {@code b} to an unknown function value. Java's {@code Consumer.accept} argument is
 * {@code @Modified}; Kotlin's {@code Function1.invoke} had no contract and read unmodified, until
 * {@code KotlinJvmFunctions} gave {@code Function0}-{@code Function3} the contract of {@code java.util.function.Function}.
 *
 * <p>The {@code suspend…} rows: a suspend function is {@code Object f(…, Continuation)} on the JVM, and a call to one
 * passes the caller's continuation. The Java side is written in that shape, and a modification must travel through
 * the suspend call exactly as through a plain one.
 */
public class TestKotlinLambdaVsJavaLambda {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKotlinLambdaVsJavaLambda.class);

    private static final String BOX = """
            package s;
            import java.util.ArrayList;
            import java.util.List;
            public class Box {
                private final List<String> items = new ArrayList<>();
                public void add(String s) { items.add(s); }
                public int size() { return items.size(); }
                public void feed(StringSink sink, String t) { sink.accept(t); }
            }
            """;

    private static final String SINK = """
            package s;
            public interface StringSink { void accept(String s); }
            """;

    private static final String KOTLIN = """
            package a
            import s.Box
            class KLam {
                fun apply(t: String, f: (String) -> Unit) { f(t) }
                fun higherOrder(b: Box, t: String) { apply(t) { s -> b.add(s) } }
                fun samConverted(b: Box, c: Box, t: String) { b.feed({ s -> c.add(s) }, t) }
                fun readOnly(b: Box, t: String): Int { apply(t) { _ -> }; return b.size() }
                fun localCaptures(b: Box, t: String) { fun add() = b.add(t); add() }
                fun localReads(b: Box): Int { fun n(): Int = b.size(); return n() }
                fun invokesValue(b: Box, f: (Box) -> Unit) { f(b) }
                suspend fun suspendModifies(b: Box, t: String) { b.add(t) }
                suspend fun suspendCaller(b: Box, t: String) { suspendModifies(b, t) }
                suspend fun suspendReads(b: Box): Int = b.size()
            }
            """;

    private static final String JAVA = """
            package b;
            import s.Box;
            import java.util.function.Consumer;
            public class JLam {
                public void apply(String t, Consumer<String> f) { f.accept(t); }
                public void higherOrder(Box b, String t) { apply(t, s -> b.add(s)); }
                public void samConverted(Box b, Box c, String t) { b.feed(s -> c.add(s), t); }
                public int readOnly(Box b, String t) { apply(t, s -> { }); return b.size(); }
                public void localCaptures(Box b, String t) { Runnable add = () -> b.add(t); add.run(); }
                public int localReads(Box b) { java.util.function.IntSupplier n = () -> b.size(); return n.getAsInt(); }
                public void invokesValue(Box b, Consumer<Box> f) { f.accept(b); }
                public Object suspendModifies(Box b, String t, kotlin.coroutines.Continuation<Object> c) { b.add(t); return null; }
                public Object suspendCaller(Box b, String t, kotlin.coroutines.Continuation<Object> c) { return suspendModifies(b, t, c); }
                public Object suspendReads(Box b, kotlin.coroutines.Continuation<Integer> c) { return b.size(); }
            }
            """;

    private static final List<String> METHODS = List.of("higherOrder", "samConverted", "readOnly", "localCaptures", "localReads", "invokesValue",
            "suspendModifies", "suspendCaller", "suspendReads");

    /**
     * ⛔ <b>The stdlib is what resolves a library call, and the fixture must prove it holds the stdlib.</b> This
     * test used to pin two "gaps" in the mixed pipeline — {@code ArrayList<String>().add(t); l.size} as three
     * placeholders, {@code f(t)} on a {@code (String) -> Unit} parameter as one — and to "refute" the stdlib as
     * their cause by adding it and seeing nothing change. Measured 2026-09-23: the jar it added was
     * {@code kotlin-stdlib-jdk8-2.4.0.jar}, the first name containing {@code kotlin-stdlib}, which holds a few
     * JDK 8 extensions and none of {@code kotlin.collections}. K2 answered {@code Unresolved reference
     * 'ArrayList'} — correctly: {@code ArrayList} is a stdlib typealias, and {@code .add} on an unresolved
     * receiver cannot resolve either. With the real jar both "gaps" convert to zero placeholders.
     *
     * <p>So the refutation measured the thing next to the question. Asserted both ways, so that neither half can
     * pass alone: without the stdlib the shapes are placeholders (the control: this test can see a hole), with
     * it they are not.
     */
    @Test
    public void theStdlibIsWhatResolvesLibraryCallsInTheMixedPipeline(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/KList.kt"), """
                package a
                class KList {
                    fun f(t: String): Int { val l = ArrayList<String>(); l.add(t); return l.size }
                    fun apply(t: String, f: (String) -> Unit) { f(t) }
                }
                """);
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");

        // ⚠ `f(t)` is deliberately NOT in the control: without the stdlib, K2 falls back to its built-in
        // `Function1`, and whether `invoke` then resolves depended on the fixture (a placeholder with `apply`
        // alone in the class, converted beside `f`). A no-stdlib project is not a real one; it is not pinned.
        PlaceholderCensus without = censusOf(kDir, jDir, false);
        assertEquals(Set.of("k2-unresolved-call:ArrayList", "k2-unresolved-call:add", "k2-unresolved-access:size"),
                without.getByKind().keySet(),
                "the control: without kotlin-stdlib these shapes cannot resolve " + without.dumpLines());
        PlaceholderCensus with = censusOf(kDir, jDir, true);
        assertEquals(0, with.getTotal(), "with kotlin-stdlib every shape converts: " + with.dumpLines());
    }

    /** Parse the fixture through the mixed pipeline, optionally putting the Kotlin stdlib on the classpath. */
    private static PlaceholderCensus censusOf(Path kDir, Path jDir, boolean withStdlib) {
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet);
        if (withStdlib) builder.addClassPath(kotlinStdlibJar());
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(builder.build());
        return PlaceholderCensus.of(parsed.getKotlinTypes());
    }

    /**
     * The kotlin-stdlib jar, taken from the K2 realm classpath this test JVM already carries
     * ({@code -Dmaddi.k2.classpath}, set by the build). ⛔ Selected by its exact file name AND checked for a class
     * only the stdlib proper holds: that classpath also carries {@code kotlin-stdlib-jdk7}/{@code -jdk8}, and a
     * {@code contains("kotlin-stdlib")} filter picked one of those — see the test above for what that cost.
     */
    private static String kotlinStdlibJar() {
        String cp = System.getProperty("maddi.k2.classpath", "");
        String jar = Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no kotlin-stdlib jar on -Dmaddi.k2.classpath (" + cp + "); this test cannot run"));
        try (java.util.jar.JarFile jf = new java.util.jar.JarFile(jar)) {
            assertNotNull(jf.getEntry("kotlin/collections/CollectionsKt.class"), jar + " is not the Kotlin stdlib");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return jar;
    }
    @Test
    public void aKotlinLambdaReachesTheSameVerdictAsTheJavaOne(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/KLam.kt"), KOTLIN);
        Files.writeString(jDir.resolve("s/Box.java"), BOX);
        Files.writeString(jDir.resolve("s/StringSink.java"), SINK);
        Files.writeString(jDir.resolve("b/JLam.java"), JAVA);

        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet)
                .addClassPath(kotlinStdlibJar()) // ⛔ without it `ArrayList`/`f(t)` are placeholders: see the test above
                .build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        if (census.getTotal() > 0) LOGGER.error("SITES {}", census.dumpLines());
        assertEquals(0, census.getTotal(), "unread Kotlin would make this vacuous: " + census.getByKind());

        new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        assertEquals(List.of(), prepAnalyzer.exceptions().stream().map(String::valueOf).toList(),
                "prep must isolate nothing");
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        new IteratingAnalyzerImpl(parsed.getJavaInspector(), new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                .build()).analyze(order, callGraph);

        TypeInfo k = type(primaryTypes, "a.KLam");
        TypeInfo j = type(primaryTypes, "b.JLam");
        StringBuilder report = new StringBuilder("\n");
        StringBuilder kotlinSide = new StringBuilder();
        StringBuilder javaSide = new StringBuilder();
        for (String name : METHODS) {
            String kv = verdict(method(k, name));
            String jv = verdict(method(j, name));
            report.append(String.format("%-14s kotlin: %-42s java: %s%n", name, kv, jv));
            kotlinSide.append(name).append(' ').append(kv).append('\n');
            javaSide.append(name).append(' ').append(jv).append('\n');
        }
        LOGGER.info("Kotlin lambda vs Java lambda:{}", report);
        assertEquals(javaSide.toString(), kotlinSide.toString(),
                "a Kotlin lambda must reach the same verdict as the Java lambda it is written to mirror");
    }

    private static String verdict(MethodInfo method) {
        boolean nonModifying = method.analysis()
                .getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
        String params = method.parameters().stream()
                .filter(p -> "s.Box".equals(String.valueOf(p.parameterizedType().typeInfo())))
                .map(p -> p.simpleName() + ".unmodified=" + p.analysis()
                        .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue())
                .collect(Collectors.joining(" "));
        return "nonModifying=" + nonModifying + " " + params;
    }

    private static TypeInfo type(Set<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in "
                        + types.stream().map(TypeInfo::fullyQualifiedName).sorted().toList()));
    }

    private static MethodInfo method(TypeInfo type, String name) {
        return type.methodStream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no method " + name + " on " + type));
    }
}
