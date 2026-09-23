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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A contract written as an annotation on Kotlin must mean what the same annotation means on Java. The analyzer's
 * contracts ride on annotations ({@code ContractResolution}, {@code AnnotationToProperty}), and until the Kotlin front
 * end converted them every Kotlin contract was silently ignored.
 *
 * <p>Each side declares its own interface — the Java set cannot see the Kotlin one — with the same annotations, and a
 * caller that uses it. Abstract methods are the point: with no body, the contract is the ONLY source of a verdict.
 * ⛔ Every contracted row has an unannotated twin, and the test asserts the two DIFFER on the Java side: a contract
 * that changes nothing would make the Kotlin/Java agreement vacuous, since both would merely agree on the default.
 */
public class TestKotlinContractsVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKotlinContractsVsJava.class);

    private static final String BOX = """
            package s;
            import java.util.ArrayList;
            import java.util.List;
            public class Box {
                private final List<String> items = new ArrayList<>();
                public void add(String s) { items.add(s); }
                public int size() { return items.size(); }
            }
            """;

    private static final String KOTLIN = """
            package a
            import s.Box
            import io.codelaser.maddi.annotation.Modified
            import io.codelaser.maddi.annotation.NotModified
            interface KSink {
                fun take(@Modified b: Box)
                fun takeNotModified(@NotModified b: Box)
                fun takePlain(b: Box)
                @NotModified fun peek()
                fun peekPlain()
            }
            class KUse {
                fun paramContract(s: KSink, b: Box) { s.take(b) }
                fun paramNotModified(s: KSink, b: Box) { s.takeNotModified(b) }
                fun paramPlain(s: KSink, b: Box) { s.takePlain(b) }
                fun methodContract(s: KSink) { s.peek() }
                fun methodPlain(s: KSink) { s.peekPlain() }
            }
            """;

    private static final String JAVA_SINK = """
            package b;
            import s.Box;
            import io.codelaser.maddi.annotation.Modified;
            import io.codelaser.maddi.annotation.NotModified;
            public interface JSink {
                void take(@Modified Box b);
                void takeNotModified(@NotModified Box b);
                void takePlain(Box b);
                @NotModified void peek();
                void peekPlain();
            }
            """;

    private static final String JAVA_USE = """
            package b;
            import s.Box;
            public class JUse {
                public void paramContract(JSink s, Box b) { s.take(b); }
                public void paramNotModified(JSink s, Box b) { s.takeNotModified(b); }
                public void paramPlain(JSink s, Box b) { s.takePlain(b); }
                public void methodContract(JSink s) { s.peek(); }
                public void methodPlain(JSink s) { s.peekPlain(); }
            }
            """;

    private static final List<String> CALLERS = List.of("paramContract", "paramNotModified", "paramPlain", "methodContract",
            "methodPlain");
    private static final List<String> SINK_METHODS = List.of("take", "takeNotModified", "takePlain", "peek", "peekPlain");

    @Test
    public void aKotlinContractMeansWhatTheJavaOneMeans(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("s/Box.java"), BOX);
        Files.writeString(jDir.resolve("b/JSink.java"), JAVA_SINK);
        Files.writeString(jDir.resolve("b/JUse.java"), JAVA_USE);

        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        // the annotations' own jar, as a project that uses them has it on its class path
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet)
                .addClassPath(annotationJar()).build();

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

        StringBuilder report = new StringBuilder("\n");
        StringBuilder kotlinSide = new StringBuilder();
        StringBuilder javaSide = new StringBuilder();
        TypeInfo kSink = type(primaryTypes, "a.KSink");
        TypeInfo jSink = type(primaryTypes, "b.JSink");
        for (String name : SINK_METHODS) row(report, kotlinSide, javaSide, name, method(kSink, name), method(jSink, name));
        TypeInfo kUse = type(primaryTypes, "a.KUse");
        TypeInfo jUse = type(primaryTypes, "b.JUse");
        for (String name : CALLERS) row(report, kotlinSide, javaSide, name, method(kUse, name), method(jUse, name));
        LOGGER.info("Kotlin contract vs Java contract:{}", report);

        // ⛔ identity: each contract must MOVE the Java verdict, or its row proves nothing about annotations
        assertNotEquals(verdict(method(jSink, "take")), verdict(method(jSink, "takePlain")),
                "the parameter contract changes nothing on the Java side; the comparison would be vacuous" + report);
        assertNotEquals(verdict(method(jSink, "peek")), verdict(method(jSink, "peekPlain")),
                "the method contract changes nothing on the Java side; the comparison would be vacuous" + report);

        // ⛔ KNOWN JAVA-ENGINE DEFECT, pinned as it is (both languages agree on the wrong answer): a @NotModified
        // contract on an abstract method's parameter makes the CALLER's argument MODIFIED, where the unannotated
        // call leaves it unmodified. The abstract method's link summary is computed while the parameter is still
        // undecided (read as dependent: `b.§m ≡ this*.§m`), and methodLinks retention keeps that richer value over
        // the recomputed `[-]` once the parameter is decided @Independent. The fix is parked on branch
        // park/abstract-summary-latest-wins, waiting on the ws/dsl work on abstract methods without
        // implementations (it unmasks an optimistic default there). When it lands, this flips to assertEquals.
        assertNotEquals(verdict(method(jUse, "paramPlain")), verdict(method(jUse, "paramNotModified")),
                "the @NotModified-parameter defect no longer reproduces: switch this to assertEquals" + report);

        assertEquals(javaSide.toString(), kotlinSide.toString(),
                "a Kotlin contract annotation must yield the verdicts of the same annotation in Java");
    }

    private static void row(StringBuilder report, StringBuilder kotlinSide, StringBuilder javaSide, String name,
                            MethodInfo k, MethodInfo j) {
        String kv = verdict(k);
        String jv = verdict(j);
        report.append(String.format("%-15s kotlin: %-42s java: %s%n", name, kv, jv));
        kotlinSide.append(name).append(' ').append(kv).append('\n');
        javaSide.append(name).append(' ').append(jv).append('\n');
    }

    /** nonModifying, plus every parameter's unmodified verdict, by position (the names differ between sides). */
    private static String verdict(MethodInfo method) {
        boolean nonModifying = method.analysis()
                .getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
        String params = method.parameters().stream()
                .map(p -> "p" + p.index() + ".unmodified=" + p.analysis()
                        .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue())
                .collect(Collectors.joining(" "));
        return "nonModifying=" + nonModifying + " " + params;
    }

    /** The jar holding io.codelaser.maddi.annotation, located from a class it contains. */
    private static String annotationJar() throws Exception {
        return Path.of(io.codelaser.maddi.annotation.NotModified.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toString();
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
