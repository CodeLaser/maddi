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
 * A field only READ through a stdlib collection extension -- {@code s.joinToString()}, {@code s.isNotEmpty()},
 * {@code l.firstOrNull()}, {@code s.mapNotNull { .. }}, ... -- must stay unmodified, as the same read in Java does.
 * Uncontracted, each of these extensions MODIFIES its receiver ({@code ShallowMethodAnalyzer}: an unannotated
 * library parameter of a non-immutable type is modified), and the library-call census ranks them first among the
 * calls that hurt: 604 of the 1,739 uncontracted calls with a mutable argument, over detekt/coil/javalin, are in
 * {@code CollectionsKt___CollectionsKt} alone.
 *
 * <p>MEASURED before the contracts: 20 of these 22 read rows said modified. After: 20 unmodified, as Java; the
 * last two, @InlineOnly, once the front end lowered them.
 *
 * <p>Guarded as TestKotlinPredicatesVsJava is: a zero-placeholder census, and a control that does modify.
 */
public class TestKotlinCollectionReadsVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKotlinCollectionReadsVsJava.class);

    private static final List<String> ROWS = List.of(
            "JoinToString", "IsNotEmpty", "SingleOrNull", "FirstOrNull", "First", "LastOrNull", "Last",
            "MapNotNull", "Find", "FilterNot", "ToSet", "Count", "CountPredicate", "FlatMap", "OrEmpty", "Plus",
            "Distinct", "SortedBy", "GroupBy", "FirstPredicate", "FilterIsInstance", "Fold", "SeqFilterToList", "SeqAny",
            "SplitArr", "TrimChars", "ArrFirstOrNull", "ArrFind", "SiblingString",
            "SiblingBuilder",
            "FilterNotNull", "Flatten", "IndexOfFirst", "SetPlus", "MapValues",
            "FilterIsInstanceArr", "SeqPlus", "OrEmptyCall", "Decode", "StringBytes",
            "RangeContains", "MatchValues", "KClassName", "FileExt", "DequeFirst", "Control");

    private static final String KOTLIN = """
            package a
            class JoinToString(private val s: List<String>) { fun f(): String = s.joinToString() }
            class IsNotEmpty(private val s: Collection<String>) { fun f(): Boolean = s.isNotEmpty() }
            class SingleOrNull(private val s: List<String>) { fun f(): String? = s.singleOrNull() }
            class FirstOrNull(private val s: List<String>) { fun f(): String? = s.firstOrNull() }
            class First(private val s: List<String>) { fun f(): String = s.first() }
            class LastOrNull(private val s: List<String>) { fun f(): String? = s.lastOrNull() }
            class Last(private val s: List<String>) { fun f(): String = s.last() }
            class MapNotNull(private val s: List<String>) { fun f(): List<Int> = s.mapNotNull { it.length } }
            class Find(private val s: List<String>) { fun f(): String? = s.find { it.length == 0 } }
            class FilterNot(private val s: List<String>) { fun f(): List<String> = s.filterNot { it.length == 0 } }
            class ToSet(private val s: List<String>) { fun f(): Set<String> = s.toSet() }
            class Count(private val s: Iterable<String>) { fun f(): Int = s.count() }
            class CountPredicate(private val s: List<String>) { fun f(): Int = s.count { it.length == 0 } }
            class FlatMap(private val s: List<String>) { fun f(): List<Char> = s.flatMap { it.toList() } }
            class OrEmpty(private val s: List<String>?) { fun f(): List<String> = s.orEmpty() }
            class Plus(private val s: List<String>) { fun f(): List<String> = s.plus("x") }
            class Distinct(private val s: List<String>) { fun f(): List<String> = s.distinct() }
            class SortedBy(private val s: List<String>) { fun f(): List<String> = s.sortedBy { it.length } }
            class GroupBy(private val s: List<String>) { fun f(): Map<Int, List<String>> = s.groupBy { it.length } }
            class FirstPredicate(private val s: List<String>) { fun f(): String = s.first { it.length == 0 } }
            class FilterIsInstance(private val s: List<Any>) { fun f(): List<String> = s.filterIsInstance<String>() }
            class Fold(private val s: List<String>) { fun f(): Int = s.fold(0) { acc, x -> acc + x.length } }
            class SeqFilterToList(private val s: Sequence<String>) { fun f(): List<String> = s.filter { it.isEmpty() }.toList() }
            class SeqAny(private val s: Sequence<String>) { fun f(): Boolean = s.any { it.isEmpty() } }
            class SplitArr(private val s: Array<String>) { fun f(x: String): List<String> = x.split(*s) }
            class TrimChars(private val s: CharArray) { fun f(x: String): String = x.trim(*s) }
            class ArrFirstOrNull(private val s: Array<String>) { fun f(): String? = s.firstOrNull() }
            class ArrFind(private val s: Array<String>) { fun f(): String? = s.find { it.isEmpty() } }
            class SiblingString(private val s: String) { fun f(x: String): String = x.removeSurrounding(s) }
            class SiblingBuilder(private val s: StringBuilder) { fun f(x: String): String = x.removeSurrounding(s) }
            class FilterNotNull(private val s: List<String?>) { fun f(): List<String> = s.filterNotNull() }
            class Flatten(private val s: List<List<String>>) { fun f(): List<String> = s.flatten() }
            class IndexOfFirst(private val s: List<String>) { fun f(): Int = s.indexOfFirst { it.isEmpty() } }
            class SetPlus(private val s: Set<String>) { fun f(): Set<String> = s + "x" }
            class MapValues(private val s: Map<String, String>) { fun f(): Map<String, Int> = s.mapValues { it.value.length } }
            class FilterIsInstanceArr(private val s: Array<Any>) { fun f(): List<String> = s.filterIsInstance<String>() }
            class SeqPlus(private val s: Sequence<String>) { fun f(): List<String> = (s + "x").toList() }
            class OrEmptyCall(private val s: List<String>?) { fun g(): List<String>? = s; fun f(): List<String> = g().orEmpty() }
            class Decode(private val s: ByteArray) { fun f(): String = s.decodeToString() }
            class StringBytes(private val s: ByteArray) { fun f(): String = String(s) }
            class RangeContains(private val s: IntRange) { fun f(x: Int): Boolean = x in s }
            class MatchValues(private val s: MatchResult) { fun f(): List<String> = s.groupValues }
            class KClassName(private val s: kotlin.reflect.KClass<*>) { fun f(): String? = s.simpleName }
            class FileExt(private val s: java.io.File) { fun f(): String = s.extension }
            class DequeFirst(private val s: ArrayDeque<String>) { fun f(): String = s.first() }
            class Control(private val s: MutableList<String>) { fun f() { s.clear() } }
            """;

    private static final String JAVA = """
            package b;
            import java.util.List;
            public final class J {
                private final List<String> s;
                public J(List<String> s) { this.s = s; }
                public String f() { return String.join(", ", s); }
            }
            """;

    @Test
    public void aReadOnlyExtensionLeavesTheFieldUnmodified(@TempDir Path tmp) throws Exception {
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

        String verdicts = Stream.concat(Stream.of("b.J"), ROWS.stream().map(r -> "a." + r))
                .map(fqn -> fqn + " " + unmodified(type(primaryTypes, fqn)))
                .collect(Collectors.joining("\n"));
        LOGGER.info("field verdicts:\n{}", verdicts);
        // Negative control, run against the archive BEFORE each row's contract: SeqFilterToList, SeqAny, TrimChars,
        // ArrFirstOrNull, ArrFind, MapValues, FilterIsInstanceArr, Decode, RangeContains, FileExt and DequeFirst were
        // false, so they prove their contract. FilterNotNull, Flatten, IndexOfFirst, SetPlus, SeqPlus, OrEmptyCall,
        // MatchValues and KClassName were already true (the jdk preload below makes Iterable/Set parameters
        // unmodified by default): they guard parity, not a contract.
        // The Sibling rows call removeSurrounding, which has NO contract but lives in a part class that has some: the
        // hints compiler used to ship defaults for such a sibling computed without the jdk results (String and
        // CharSequence mutable), so both fields read false. AnalysisHintsCompiler's preloadResults fixed it.
        // isNotEmpty and orEmpty are @InlineOnly: no method for a contract to name. They were the two rows left wrong by
        // the contracts, and the front end's lowering to the call kotlinc inlines (TestInlineOnlyLowering) fixed them.
        assertEquals("""
                b.J true
                a.JoinToString true
                a.IsNotEmpty true
                a.SingleOrNull true
                a.FirstOrNull true
                a.First true
                a.LastOrNull true
                a.Last true
                a.MapNotNull true
                a.Find true
                a.FilterNot true
                a.ToSet true
                a.Count true
                a.CountPredicate true
                a.FlatMap true
                a.OrEmpty true
                a.Plus true
                a.Distinct true
                a.SortedBy true
                a.GroupBy true
                a.FirstPredicate true
                a.FilterIsInstance true
                a.Fold true
                a.SeqFilterToList true
                a.SeqAny true
                a.SplitArr true
                a.TrimChars true
                a.ArrFirstOrNull true
                a.ArrFind true
                a.SiblingString true
                a.SiblingBuilder true
                a.FilterNotNull true
                a.Flatten true
                a.IndexOfFirst true
                a.SetPlus true
                a.MapValues true
                a.FilterIsInstanceArr true
                a.SeqPlus true
                a.OrEmptyCall true
                a.Decode true
                a.StringBytes true
                a.RangeContains true
                a.MatchValues true
                a.KClassName true
                a.FileExt true
                a.DequeFirst true
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
