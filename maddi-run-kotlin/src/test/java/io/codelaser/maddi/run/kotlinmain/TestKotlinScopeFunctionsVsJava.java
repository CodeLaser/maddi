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

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
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

/**
 * The scope functions and preconditions -- {@code let}, {@code apply}, {@code also}, {@code takeIf}, {@code run},
 * {@code with}, {@code require}, {@code check}, {@code error}, {@code requireNotNull} -- are the most-called
 * uncontracted members of the Kotlin stdlib (library-call census: 732 calls in {@code StandardKt} alone over
 * detekt/coil/javalin). This pins what they mean to the analysis, contracted or not, against Java.
 *
 * <p>⭐ MEASURED, uncontracted: every read row is unmodified -- the receiver is a bare {@code T}, which the defaults
 * already treat as unmodified -- so the census's "uncontracted" does not mean "harmful" here.
 *
 * <p>⛔ The MODIFY rows ({@code s.apply { clear() }}) are unmodified TOO, and that is the engine's design, not a Kotlin
 * gap: a modification made by a functional argument is not attributed to what was handed to it. The Java twins say
 * the same through the JDK -- {@code Optional.of(s).ifPresent(x -> x.clear())}, {@code Stream.of(s).forEach(..)},
 * {@code list.forEach(..)} all leave {@code s} unmodified; only a lambda invoked directly ({@code c.accept(s)}) is
 * seen. The two languages agree, which is what this asserts. If the engine ever learns to follow an argument into a
 * library lambda, both columns move together, and this test says so.
 *
 * <p>Guarded as TestKotlinPredicatesVsJava is: a zero-placeholder census, and a control that does modify.
 */
public class TestKotlinScopeFunctionsVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKotlinScopeFunctionsVsJava.class);

    private static final List<String> ROWS = List.of(
            // reads: the lambda only reads, so the field stays unmodified
            "LetRead", "ApplyRead", "AlsoRead", "TakeIfRead", "RunRead", "WithRead",
            "RequireRead", "CheckRead", "ErrorRead", "RequireNotNullRead",
            // modifications through the lambda: unmodified, as Java's through a library lambda (see the class doc)
            "ApplyModify", "AlsoModify", "LetModify", "RunModify", "WithModify",
            // the sensor sees a modification at all
            "Control");

    private static final String KOTLIN = """
            package a
            class LetRead(private val s: MutableSet<String>) { fun f(): Int = s.let { it.size } }
            class ApplyRead(private val s: MutableSet<String>) { fun f(): Int { s.apply { size }; return 0 } }
            class AlsoRead(private val s: MutableSet<String>) { fun f(): Int { s.also { it.size }; return 0 } }
            class TakeIfRead(private val s: MutableSet<String>) { fun f(): Boolean = s.takeIf { it.isEmpty() } != null }
            class RunRead(private val s: MutableSet<String>) { fun f(): Int = s.run { size } }
            class WithRead(private val s: MutableSet<String>) { fun f(): Int = with(s) { size } }
            class RequireRead(private val s: MutableSet<String>) { fun f() { require(s.isEmpty()) { "not empty" } } }
            class CheckRead(private val s: MutableSet<String>) { fun f() { check(s.isEmpty()) { "not empty" } } }
            class ErrorRead(private val s: MutableSet<String>) { fun f(): Nothing = error(s) }
            class RequireNotNullRead(private val s: MutableSet<String>?) { fun f(): Int = requireNotNull(s).size }
            class ApplyModify(private val s: MutableSet<String>) { fun f() { s.apply { clear() } } }
            class AlsoModify(private val s: MutableSet<String>) { fun f() { s.also { it.clear() } } }
            class LetModify(private val s: MutableSet<String>) { fun f() { s.let { it.clear() } } }
            class RunModify(private val s: MutableSet<String>) { fun f() { s.run { clear() } } }
            class WithModify(private val s: MutableSet<String>) { fun f() { with(s) { clear() } } }
            class Control(private val s: MutableSet<String>) { fun f() { s.clear() } }
            """;

    private static final String JAVA = """
            package b;
            import java.util.Set;
            public final class J {
                private final Set<String> s;
                public J(Set<String> s) { this.s = s; }
                public int f() { return s.size(); }
            }
            """;

    /** Java handing the field to a lambda through a LIBRARY method: the parity question for the modify rows. */
    private static final List<String> JAVA_TWINS = List.of(
            "package b; import java.util.Set; public final class JOptionalModify { private final Set<String> s; public JOptionalModify(Set<String> s) { this.s = s; } public void f() { java.util.Optional.of(s).ifPresent(x -> x.clear()); } }",
            "package b; import java.util.Set; public final class JStreamModify { private final Set<String> s; public JStreamModify(Set<String> s) { this.s = s; } public void f() { java.util.stream.Stream.of(s).forEach(x -> x.clear()); } }",
            "package b; import java.util.Set; import java.util.function.Consumer; public final class JConsumerModify { private final Set<String> s; public JConsumerModify(Set<String> s) { this.s = s; } public void f() { Consumer<Set<String>> c = x -> x.clear(); c.accept(s); } }",
            "package b; import java.util.Set; public final class JListForEachModify { private final java.util.List<Set<String>> s; public JListForEachModify(java.util.List<Set<String>> s) { this.s = s; } public void f() { s.forEach(x -> x.clear()); } }");

    @Test
    public void aScopeFunctionModifiesOnlyWhatItsLambdaModifies(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("b/J.java"), JAVA);
        for (String twin : JAVA_TWINS) {
            String name = twin.substring(twin.indexOf("class ") + 6, twin.indexOf(" {"));
            Files.writeString(jDir.resolve("b/" + name + ".java"), twin);
        }
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), "unread Kotlin would make this vacuous: " + census.dumpLines());

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

        String verdicts = Stream.concat(Stream.of("b.J", "b.JOptionalModify", "b.JStreamModify", "b.JConsumerModify", "b.JListForEachModify"), ROWS.stream().map(r -> "a." + r))
                .map(fqn -> fqn + " " + unmodified(type(primaryTypes, fqn)))
                .collect(Collectors.joining("\n"));
        LOGGER.info("field verdicts:\n{}", verdicts);
        assertEquals("""
                b.J true
                b.JOptionalModify true
                b.JStreamModify true
                b.JConsumerModify false
                b.JListForEachModify true
                a.LetRead true
                a.ApplyRead true
                a.AlsoRead true
                a.TakeIfRead true
                a.RunRead true
                a.WithRead true
                a.RequireRead true
                a.CheckRead true
                a.ErrorRead true
                a.RequireNotNullRead true
                a.ApplyModify true
                a.AlsoModify true
                a.LetModify true
                a.RunModify true
                a.WithModify true
                a.Control false""", verdicts);
    }

    private static boolean unmodified(TypeInfo type) {
        return type.getFieldByName("s", true).analysis()
                .getOrDefault(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    private static TypeInfo type(Set<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn));
    }

    private static String kotlinStdlibJar() {
        String cp = System.getProperty("maddi.k2.classpath", "");
        return Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no kotlin-stdlib jar on -Dmaddi.k2.classpath (" + cp + "); this test cannot run"));
    }
}
