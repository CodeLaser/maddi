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
import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.modification.link.io.LinkCodec;
import io.codelaser.maddi.modification.prepwork.io.WriteAnalysisResults;
import io.codelaser.maddi.util.Trie;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⭐ Ladder rung 6: <b>a real Kotlin round trip.</b> A first session parses, analyses and WRITES; a second,
 * completely fresh session parses the same sources, does no analysis, and only LOADS what the first wrote.
 * That is exactly the incremental / IDE-daemon scenario, and it is the half that had never been shown to
 * work for Kotlin — §5.1 recorded "no encode path", meaning no Kotlin module referenced {@code Codec}.
 *
 * <p>What the probe found, in order, none of it guessable from the gap list:
 * <ol>
 *   <li>The ENCODER already handles Kotlin — the writer is language-agnostic and produced a complete
 *       {@code A.json}. "No encode path" meant nobody CALLS it, not that it cannot.</li>
 *   <li>A Kotlin type is only resolvable by FQN when the configuration also has a Java source set; with a
 *       Kotlin-only configuration {@code runtime.getFullyQualified} returns null and every hint is skipped
 *       as "type not on the classpath".</li>
 *   <li>⛔ Two properties the analysers WRITE were unknown to the decoder, which does not degrade: it
 *       asserts, and the whole file is lost. Fixed in {@code PropertyProviderImpl}, with
 *       {@code TestEveryWritablePropertyDecodes} pinning the class of defect. Java-side, both of them.</li>
 *   <li>The reader must be paired with the writer: {@code LoadAnalysisResults} lives in
 *       maddi-modification-prepwork, which structurally cannot know {@code methodLinks} (declared in
 *       maddi-modification-link). {@code LinkCodec.restoreCodec()} is the matching read side.</li>
 * </ol>
 *
 * <p>⚠ What this does NOT yet show: the mixed CLI still writes nothing. {@code RunMixedPrepAnalyzer} has no
 * results-directory option, so incremental analysis and the IDE daemon remain unwired for Kotlin — but the
 * mechanism underneath them is now demonstrated rather than assumed.
 */
public class TestKotlinAnalysisRoundTrip {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestKotlinAnalysisRoundTrip.class);

    private static final String KOTLIN = """
            package a
            class KBox {
                private var count: Int = 0
                fun add(n: Int) { count += n }
                fun size(): Int = count
            }
            """;

    @Test
    public void aKotlinAnalysisSurvivesAWriteAndAReload(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/KBox.kt"), KOTLIN);
        // ⚠ a Java type in the same run, as the CONTROL for the lookup below: it tells apart "this runtime
        // registers nothing" from "the Kotlin front end registers nothing".
        Files.writeString(jDir.resolve("b/JBox.java"), """
                package b;
                public class JBox {
                    private int count;
                    public void add(int n) { count += n; }
                    public int size() { return count; }
                }
                """);

        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), "unread Kotlin: " + census.getByKind());

        new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        assertEquals(List.of(), prepAnalyzer.exceptions().stream().map(String::valueOf).toList());
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        new IteratingAnalyzerImpl(parsed.getJavaInspector(), new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                .build()).analyze(order, callGraph);

        TypeInfo kBox = primaryTypes.stream().filter(t -> "a.KBox".equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow();
        MethodInfo add = method(kBox, "add");
        MethodInfo size = method(kBox, "size");
        LOGGER.info("before the round trip: add.nonModifying={} size.nonModifying={}",
                nonModifying(add), nonModifying(size));

        Path out = tmp.resolve("results");
        Files.createDirectories(out);
        Trie<TypeInfo> trie = new Trie<>();
        primaryTypes.forEach(ti -> trie.add(ti.packageName().split("\\."), ti));
        // ⛔ The codec is not a detail. `LoadAnalysisResults` lives in maddi-modification-prepwork, whose
        // property provider structurally CANNOT know `methodLinks` — that Property is declared in
        // maddi-modification-link, which prepwork does not (and must not) depend on. Pairing the full writer
        // with the hints reader fails with "Have no property object for key methodLinks". LinkCodec is the
        // matching pair, and `restoreCodec()` is its read side.
        new WriteAnalysisResults(runtime).write(out.toFile(), trie,
                new LinkCodec(parsed.getJavaInspector(), kotlinSet).codec());

        List<Path> written;
        try (Stream<Path> walk = Files.walk(out)) {
            written = walk.filter(Files::isRegularFile).toList();
        }
        LOGGER.info("ENCODE wrote {} file(s): {}", written.size(), written);
        assertTrue(written.size() >= 1, "the encode path produced nothing at all");
        String json = Files.readString(written.getFirst());
        LOGGER.info("ENCODE content ({} chars): {}", json.length(), json.length() > 1500 ? json.substring(0, 1500) : json);
        assertTrue(json.contains("KBox"), "the written result does not mention the Kotlin type: " + json);

        // ⭐ THE ROUND TRIP. A second, completely fresh session parses the same sources and does NO analysis;
        // it only LOADS what the first one wrote. That is exactly the incremental / IDE-daemon scenario, and
        // it is the half that was never shown to work for Kotlin.
        MixedProjectInspector.Result reparsed = new MixedProjectInspector().parse(config);
        Runtime runtime2 = reparsed.getRuntime();
        TypeInfo kBox2 = reparsed.getKotlinTypes().stream().map(TypeInfo::primaryType)
                .filter(t -> "a.KBox".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        // ⛔ the negative control, and it took two attempts to state correctly. A freshly parsed type does not
        // carry NO verdicts — it carries DEFAULTS (nonModifying=false, unmodified=false), which is precisely
        // what a decode that silently did nothing would leave behind. So the control is that the fresh
        // fingerprint DIFFERS from the analysed one; only then does matching it after the load mean anything.
        String before = fingerprint(kBox);
        String freshlyParsed = fingerprint(kBox2);
        LOGGER.info("ROUNDTRIP fresh  : {}", freshlyParsed);
        assertNotEquals(before, freshlyParsed,
                "a freshly parsed type already agrees with the analysed one, so loading cannot be shown to do anything");

        LOGGER.info("PROBE kotlin a.KBox via kotlinSet = {}", runtime2.getFullyQualified("a.KBox", false, kotlinSet));
        LOGGER.info("PROBE kotlin a.KBox via javaSet   = {}", runtime2.getFullyQualified("a.KBox", false, javaSet));
        LOGGER.info("PROBE java   b.JBox via javaSet   = {}", runtime2.getFullyQualified("b.JBox", false, javaSet));
        LOGGER.info("PROBE java   b.JBox via kotlinSet = {}", runtime2.getFullyQualified("b.JBox", false, kotlinSet));
        Codec restore = new LinkCodec(reparsed.getJavaInspector(), kotlinSet).restoreCodec();
        int loaded = new LoadAnalysisResults(runtime2, kotlinSet).go(restore, List.of(out.toString()));
        LOGGER.info("DECODE loaded {} primary type(s) from {}", loaded, out);
        assertEquals(2, loaded, "the decode path must read back both the Kotlin type and the Java control");

        String after = fingerprint(kBox2);
        LOGGER.info("ROUNDTRIP before: {}", before);
        LOGGER.info("ROUNDTRIP after : {}", after);
        assertEquals(before, after, "a Kotlin type's verdicts must survive the round trip unchanged");
    }

    /**
     * Every member's verdicts, in a stable order: what a reader of the results would actually consume.
     * ⚠ The first draft filtered out lines reading {@code nonModifying=false params=}, meaning to drop
     * empty ones — it dropped {@code add}, the only modifying method, and left a "round trip" that compared
     * one read-only method with itself. A fingerprint that hides the interesting member proves nothing.
     */
    private static String fingerprint(TypeInfo type) {
        return type.methodStream()
                .sorted(java.util.Comparator.comparing(MethodInfo::name))
                .map(m -> m.name() + " nonModifying=" + nonModifying(m)
                          + " params=" + m.parameters().stream()
                                  .map(p -> p.simpleName() + ":" + p.analysis()
                                          .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue())
                                  .collect(Collectors.joining(",")))
                .collect(Collectors.joining("; "));
    }

    private static boolean nonModifying(MethodInfo method) {
        return method.analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    private static MethodInfo method(TypeInfo type, String name) {
        return type.methodStream().filter(m -> name.equals(m.name())).findFirst().orElseThrow();
    }
}
