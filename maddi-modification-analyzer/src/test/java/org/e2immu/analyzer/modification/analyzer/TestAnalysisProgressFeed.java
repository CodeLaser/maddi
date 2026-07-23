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

package org.e2immu.analyzer.modification.analyzer;

import org.e2immu.language.cst.api.info.Info;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify the instrument before trusting its numbers: the feed must sample heap and GC beans without
 * throwing, account for elements across waves, and emit one well-formed JSONL record per heartbeat.
 * The feed only reads {@code analyzed.size()}, so the collections here hold nulls of the right length.
 */
public class TestAnalysisProgressFeed {

    private static List<Info> sized(int n) {
        List<Info> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(null); // size is all the feed reads
        return list;
    }

    @Test
    public void heartbeatEmitsJsonlAndCountsElements() throws Exception {
        Path dir = Files.createTempDirectory("progress-feed-test");
        File metrics = new File(dir.toFile(), "metrics.jsonl");
        // heartbeat 0 => every wave emits, so we can assert one line per wave deterministically
        AnalysisProgressFeed feed = new AnalysisProgressFeed(1000, metrics, 0);

        feed.waveCompleted(1, 1, sized(400));
        feed.waveCompleted(1, 2, sized(350));
        feed.waveCompleted(1, 3, sized(250));
        feed.passCompleted(1, true, sized(1000));

        assertTrue(metrics.exists(), "metrics.jsonl should have been written");
        List<String> lines = Files.readAllLines(metrics.toPath());
        assertEquals(4, lines.size(), "three waves + one pass boundary");

        // every line is a flat JSON object carrying the load-bearing keys
        for (String line : lines) {
            assertTrue(line.startsWith("{") && line.endsWith("}"), () -> "not a JSON object: " + line);
            for (String key : List.of("\"t\":", "\"done\":", "\"total\":1000", "\"heapUsedMB\":",
                    "\"gcFraction\":", "\"rate\":", "\"etaSec\":")) {
                assertTrue(line.contains(key), () -> "missing " + key + " in " + line);
            }
        }
        // element accounting: waves sum to 1000, and the first full pass pins done to the total
        assertTrue(lines.get(2).contains("\"done\":1000"), () -> "waves should sum to total: " + lines.get(2));
        assertTrue(lines.get(3).contains("\"done\":1000"), () -> "pass boundary pins to total: " + lines.get(3));
        assertTrue(feed.peakUsedBytes() > 0, "should have sampled a non-zero heap");
    }

    @Test
    public void elementCompletedAdvancesProgressInsideAGiantWave() throws Exception {
        // the miles-core case: after a small wave commits, the analysis enters ONE giant SCC wave whose
        // barrier fires only at its end. elementCompleted() must advance 'done' in between, so progress is
        // not pinned for the whole (multi-hour) wave. Barrier then commits and resets the in-flight tally.
        Path dir = Files.createTempDirectory("progress-feed-intrawave");
        File metrics = new File(dir.toFile(), "metrics.jsonl");
        AnalysisProgressFeed feed = new AnalysisProgressFeed(1000, metrics, 0); // heartbeat 0 => emit on every event

        feed.waveCompleted(1, 1, sized(200));                                   // commit 200
        for (int i = 0; i < 500; i++) feed.elementCompleted();                  // 500 of the giant wave done, no barrier
        feed.phase(AnalysisValueFeed.Phase.CYCLE_BREAKING_ACTIVATED, 1);        // mid-wave emit
        for (int i = 0; i < 300; i++) feed.elementCompleted();                  // remaining 300 (giant wave = 800)
        feed.waveCompleted(1, 2, sized(800));                                   // barrier commits the 800

        List<String> lines = Files.readAllLines(metrics.toPath());
        assertEquals(3, lines.size(), "wave1 + mid-wave phase + giant-wave barrier");
        assertTrue(lines.get(0).contains("\"done\":200"), () -> "first wave commits 200: " + lines.get(0));
        assertTrue(lines.get(1).contains("\"done\":700"),
                () -> "intra-wave = 200 committed + 500 in-flight: " + lines.get(1));
        assertTrue(lines.get(2).contains("\"done\":1000"),
                () -> "barrier commits total and resets in-flight: " + lines.get(2));
    }

    @Test
    public void samplerEmitsWithNoWaveOrPassEvents() throws Exception {
        // the giant-SCC case: pass 1 stays inside one long wave, so no wave/pass callback fires. The
        // time-based sampler must still produce heap/GC heartbeats so the run is never blind.
        Path dir = Files.createTempDirectory("progress-feed-sampler");
        File metrics = new File(dir.toFile(), "metrics.jsonl");
        AnalysisProgressFeed feed = new AnalysisProgressFeed(500_000, metrics, 50);
        try {
            Thread.sleep(350); // ~several 50ms heartbeats, no events at all
        } finally {
            feed.stop();
        }
        assertTrue(metrics.exists(), "sampler should have written heartbeats");
        List<String> lines = Files.readAllLines(metrics.toPath());
        assertFalse(lines.isEmpty(), "at least one sampling heartbeat expected");
        assertTrue(lines.stream().allMatch(l -> l.contains("\"where\":\"sampling\"")),
                () -> "all lines should be sampler emissions: " + lines);
        assertTrue(lines.stream().allMatch(l -> l.contains("\"heapUsedMB\":")),
                "sampler heartbeats carry heap");
    }

    @Test
    public void nullMetricsFileIsLogOnlyAndSafe() {
        // no file: must still sample heap/GC and not throw
        AnalysisProgressFeed feed = new AnalysisProgressFeed(10, null, 0);
        assertDoesNotThrow(() -> {
            feed.waveCompleted(1, 1, sized(10));
            feed.passCompleted(1, true, sized(10));
            feed.phase(AnalysisValueFeed.Phase.TERMINAL_CERTIFIED, 3);
        });
    }
}
