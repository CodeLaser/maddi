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

package org.e2immu.analyzer.modification.analyzer.integration;

import org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed;
import org.e2immu.analyzer.modification.analyzer.CheckpointWriter;
import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.link.io.LinkCodec;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #34 (checkpoint/resume v1): the {@link CheckpointWriter} persists analyzed values at pass
 * boundaries; restore preloads them via {@link LoadAnalysisResults} and re-runs the analyzer, whose
 * verify-certify sweep is the soundness net. This pins the full round trip: analyze → checkpoint →
 * lose everything → restore → re-certify with identical verdicts.
 */
public class TestCheckpointResume extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                private final List<String> list = new ArrayList<>();
                int size() { return list.size(); }
                void add(String s) { list.add(s); }
                static String twice(String s) { return s + s; }
            }
            """;

    @DisplayName("analyze -> checkpoint -> restore -> re-certify with identical verdicts")
    @Test
    public void test() throws IOException {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);

        File dir = new File("build/json-checkpoint");
        LinkCodec linkCodec = new LinkCodec(javaInspector);
        CheckpointWriter writer = new CheckpointWriter(runtime, linkCodec::codec, dir);
        List<AnalysisValueFeed.Phase> phases = new ArrayList<>();
        AnalysisValueFeed feed = new AnalysisValueFeed() {
            @Override
            public void passCompleted(int iteration, boolean fullPass, java.util.Collection<Info> analyzed) {
                writer.passCompleted(iteration, fullPass, analyzed);
            }

            @Override
            public void phase(Phase phase, int iteration) {
                phases.add(phase);
                writer.phase(phase, iteration);
            }
        };
        IteratingAnalyzer iterating = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        iterating.setValueFeed(feed);
        iterating.analyze(analysisOrder);

        assertTrue(phases.contains(AnalysisValueFeed.Phase.TERMINAL_CERTIFIED));
        assertTrue(writer.typesWritten() > 0, "the checkpoint must have written at least one type");
        assertTrue(new File(dir, "checkpoint-terminal.txt").exists(), "terminal marker must be written");

        Map<String, String> before = verdicts(X);
        assertFalse(before.isEmpty());

        // "crash": lose all in-memory analysis by re-parsing from scratch
        javaInspector.invalidateAllSources();
        TypeInfo X2 = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder2 = prepWork(X2);

        // restore: preload the checkpointed values (present values win — prep recomputed some), then
        // re-run — the verify-certify sweep is the net
        int loaded = new LoadAnalysisResults(runtime, javaInspector.mainSources())
                .goDir(new LinkCodec(javaInspector).restoreCodec(), dir);
        assertTrue(loaded > 0, "restore must load the checkpointed files");

        IteratingAnalyzer iterating2 = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        iterating2.analyze(analysisOrder2);

        Map<String, String> after = verdicts(X2);
        assertEquals(before, after, "restored + re-certified verdicts must be identical");
    }

    @DisplayName("wave-boundary delta writes during the first pass (checkpoint granularity gap)")
    @Test
    public void testWaveBoundary() throws IOException {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        IteratingAnalyzer iterating = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        iterating.analyze(analysisOrder);

        // throttled: within the interval, waves only accumulate — nothing on disk
        File throttledDir = new File("build/json-checkpoint-wave-throttled");
        deleteJsonFiles(throttledDir);
        LinkCodec linkCodec = new LinkCodec(javaInspector);
        CheckpointWriter throttled = new CheckpointWriter(runtime, linkCodec::codec, throttledDir, Long.MAX_VALUE);
        throttled.waveCompleted(1, 1, analysisOrder);
        assertEquals(0, throttled.typesWritten(), "within the interval, the wave must only accumulate");

        // interval 0: every wave flushes immediately
        File waveDir = new File("build/json-checkpoint-wave");
        deleteJsonFiles(waveDir);
        CheckpointWriter perWave = new CheckpointWriter(runtime, linkCodec::codec, waveDir, 0);
        perWave.waveCompleted(1, 1, analysisOrder);
        assertTrue(perWave.typesWritten() > 0, "interval 0 must flush the wave delta");
        File[] json = waveDir.listFiles((_, name) -> name.endsWith(".json"));
        assertNotNull(json);
        assertTrue(json.length > 0, "the wave delta must be on disk");
    }

    private static void deleteJsonFiles(File dir) {
        File[] files = dir.listFiles((_, name) -> name.endsWith(".json"));
        if (files != null) for (File f : files) assertTrue(f.delete());
    }

    private Map<String, String> verdicts(TypeInfo typeInfo) {
        Map<String, String> map = new LinkedHashMap<>();
        typeInfo.recursiveSubTypeStream().forEach(ti -> {
            var imm = ti.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                    org.e2immu.language.cst.api.analysis.Value.Immutable.class);
            map.put(ti.fullyQualifiedName(), "immutable=" + imm);
            ti.constructorAndMethodStream().forEach(mi -> map.put(mi.fullyQualifiedName(),
                    "nonModifying=" + nonModifying(mi)));
        });
        return map;
    }

    private String nonModifying(MethodInfo mi) {
        var v = mi.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD,
                org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.class);
        return v == null ? "null" : String.valueOf(v.isTrue());
    }
}
