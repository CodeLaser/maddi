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
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
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
 * ⭐ <b>Does the modification analysis reach the same conclusion about a lowered Kotlin shape as about the
 * Java a human would write for it?</b>
 *
 * <p>The statements-in-expression-position family is a set of LOWERINGS: `try` used as a value, an `if`
 * whose branch is a block, `x ?: return`, a null-safe chain. Each rewrites Kotlin into a CST shape Java can
 * express. Everything that had been measured about them until now was about the parse — placeholders,
 * statement indexes, prep isolating nothing. None of that says the analyzer concludes the RIGHT thing, and a
 * lowering can be well formed and wrong: `val t = f() ?: return` evaluated `f()` twice for months without a
 * single red test.
 *
 * <p>So both sides call the SAME Java helper ({@code s.Box}) and differ only in the shape. Any disagreement
 * is the lowering, not the standard library.
 *
 * <h2>What is asserted</h2>
 * Per method pair: whether the method is non-modifying, and whether its {@code Box} parameter is left
 * unmodified. Those two are the sensors that a mis-shaped tree moves — a modification that happens in a
 * `catch` arm, or through a link the lowering broke, shows up here and almost nowhere else.
 */
public class TestLoweredShapesVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestLoweredShapesVsJava.class);

    /** Called identically from both sides, so the comparison is about shape and nothing else. */
    private static final String BOX = """
            package s;
            import java.util.ArrayList;
            import java.util.List;
            public class Box {
                private final List<String> items = new ArrayList<>();
                public void add(String s) { items.add(s); }
                public int size() { return items.size(); }
                public Box next() { return this; }
            }
            """;

    private static final String KOTLIN = """
            package a
            import s.Box
            class K {
                fun tryAsValue(b: Box, t: String): Int {
                    val v = try { b.size() } catch (e: RuntimeException) { b.add(t); -1 }
                    return v
                }
                fun ifAsValue(b: Box, t: String, c: Boolean): Int {
                    val v = if (c) { b.add(t); 1 } else { b.size() }
                    return v
                }
                fun elvisGuard(b: Box?, t: String): Int {
                    val x = b ?: return 0
                    x.add(t)
                    return x.size()
                }
                fun safeChain(b: Box?, t: String): Int {
                    b?.next()?.add(t)
                    return 0
                }
                fun readOnlyChain(b: Box?): Int = b?.next()?.size() ?: 0
            }
            """;

    /** The same five, as a human writes them in Java — which is what each lowering claims to produce. */
    private static final String JAVA = """
            package b;
            import s.Box;
            public class J {
                public int tryAsValue(Box b, String t) {
                    int v;
                    try { v = b.size(); } catch (RuntimeException e) { b.add(t); v = -1; }
                    return v;
                }
                public int ifAsValue(Box b, String t, boolean c) {
                    int v;
                    if (c) { b.add(t); v = 1; } else { v = b.size(); }
                    return v;
                }
                public int elvisGuard(Box b, String t) {
                    if (b == null) return 0;
                    Box x = b;
                    x.add(t);
                    return x.size();
                }
                public int safeChain(Box b, String t) {
                    Box t1 = b == null ? null : b.next();
                    if (t1 != null) t1.add(t);
                    return 0;
                }
                public int readOnlyChain(Box b) {
                    Box t1 = b == null ? null : b.next();
                    return t1 == null ? 0 : t1.size();
                }
            }
            """;

    private static final List<String> METHODS =
            List.of("tryAsValue", "ifAsValue", "elvisGuard", "safeChain", "readOnlyChain");

    @Test
    public void everyLoweredShapeAgreesWithTheJavaItClaimsToProduce(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("b/J.java"), JAVA);
        Files.writeString(jDir.resolve("s/Box.java"), BOX);

        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        // without the annotated APIs java.util.List is an unknown and NOTHING can be concluded on either
        // side, which would make this comparison vacuously equal
        new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        assertEquals(List.of(), prepAnalyzer.exceptions().stream().map(String::valueOf).toList(),
                "prep must isolate nothing: an isolated element is an unanswered question, not an answer");
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        new IteratingAnalyzerImpl(parsed.getJavaInspector(), new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                .build()).analyze(order, callGraph);

        TypeInfo k = type(primaryTypes, "a.K");
        TypeInfo j = type(primaryTypes, "b.J");
        StringBuilder report = new StringBuilder("\n");
        StringBuilder kotlinSide = new StringBuilder();
        StringBuilder javaSide = new StringBuilder();
        for (String name : METHODS) {
            String kv = verdict(method(k, name));
            String jv = verdict(method(j, name));
            report.append(String.format("%-14s kotlin: %-44s java: %s%n", name, kv, jv));
            kotlinSide.append(name).append(' ').append(kv).append('\n');
            javaSide.append(name).append(' ').append(jv).append('\n');
        }
        LOGGER.info("lowered shape vs hand-written Java:{}", report);
        assertEquals(javaSide.toString(), kotlinSide.toString(),
                "a lowered Kotlin shape must yield the same verdicts as the Java it claims to produce");
    }

    private static TypeInfo type(Set<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in "
                        + types.stream().map(TypeInfo::fullyQualifiedName).sorted().toList()));
    }

    private static MethodInfo method(TypeInfo type, String name) {
        return type.methods().stream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no method " + name + " on " + type.fullyQualifiedName()));
    }

    /** The two sensors a mis-shaped tree actually moves. */
    private static String verdict(MethodInfo method) {
        boolean nonModifying = method.analysis()
                .getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
        ParameterInfo box = method.parameters().getFirst();
        Value.Bool unmodified = box.analysis()
                .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE);
        return "nonModifying=" + nonModifying + " box.unmodified=" + unmodified.isTrue();
    }
}
