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
 * A read-only predicate over a field must leave the field unmodified, as Java's {@code stream().anyMatch(..)} does.
 * Found through local functions: detekt's {@code PathFilters.isIgnored} is
 * {@code fun isIncluded() = includes?.any { it.matches(path) } ?: true}, and once local-function bodies were read,
 * {@code includes} went modified, and with it, through the fields that hold a {@code PathFilters}, {@code Analyzer}
 * and {@code Lifecycle}. The local function was innocent: the same body inline gave the same verdict.
 * {@code Iterable.any} had no contract, and an uncontracted receiver is a modified one. {@code any}, {@code all}
 * and {@code none} are contracted in {@code KotlinCollections} now.
 *
 * <p>Guarded twice: a zero-placeholder census (an unread body agrees with anything), and a control class whose method
 * really does modify the field, so the sensor is shown to see a modification at all.
 */
public class TestKotlinPredicatesVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKotlinPredicatesVsJava.class);

    private static final String KOTLIN = """
            package a
            import java.nio.file.Path
            import java.nio.file.PathMatcher
            class KAny(private val includes: Set<PathMatcher>?) {
                fun isIgnored(path: Path): Boolean = !(includes?.any { it.matches(path) } ?: true)
            }
            class KAll(private val includes: Set<PathMatcher>) {
                fun isIgnored(path: Path): Boolean = includes.all { !it.matches(path) }
            }
            class KNone(private val includes: Set<PathMatcher>) {
                fun isIgnored(path: Path): Boolean = includes.none() || includes.none { it.matches(path) }
            }
            class KControl(private val includes: MutableSet<PathMatcher>) {
                fun isIgnored(path: Path): Boolean { includes.clear(); return true }
            }
            """;

    private static final String JAVA = """
            package b;
            import java.nio.file.Path;
            import java.nio.file.PathMatcher;
            import java.util.Set;
            public final class J {
                private final Set<PathMatcher> includes;
                public J(Set<PathMatcher> includes) { this.includes = includes; }
                public boolean isIgnored(Path path) {
                    return !(includes == null || includes.stream().anyMatch(m -> m.matches(path)));
                }
            }
            """;

    @Test
    public void aReadOnlyPredicateLeavesTheFieldUnmodified(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("b/J.java"), JAVA);
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

        String verdicts = Stream.of("b.J", "a.KAny", "a.KAll", "a.KNone", "a.KControl")
                .map(fqn -> fqn + " " + unmodified(type(primaryTypes, fqn)))
                .collect(Collectors.joining("\n"));
        LOGGER.info("field verdicts:\n{}", verdicts);
        assertEquals("""
                b.J true
                a.KAny true
                a.KAll true
                a.KNone true
                a.KControl false""", verdicts);
    }

    private static boolean unmodified(TypeInfo type) {
        return type.getFieldByName("includes", true).analysis()
                .getOrDefault(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    private static TypeInfo type(Set<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn));
    }

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
}
