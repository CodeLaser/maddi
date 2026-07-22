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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;

/**
 * Observability for the multi-hour analysis run: an {@link AnalysisValueFeed} that answers, on a
 * throttled heartbeat, the three questions a 5-8h (and, for the 3M-line target, longer) run leaves you
 * asking — <em>where are we, how are we doing, what to expect.</em>
 *
 * <p><b>Where.</b> The heavy stretch is the cold FIRST pass, which the analyzer runs as strata waves
 * (see {@link org.e2immu.analyzer.modification.analyzer.impl.SingleIterationAnalyzerImpl}); this feed
 * counts elements at each {@link #waveCompleted} barrier, so progress advances during the very stretch
 * that pass-boundary logging leaves silent. Later (worklist) passes are fast and reported at their
 * boundary only.
 *
 * <p><b>How.</b> Each heartbeat samples the heap ({@link MemoryMXBean}) and the collectors
 * ({@link GarbageCollectorMXBean}) and reports the <em>GC-time fraction of the interval</em>. That
 * fraction is the early-warning the elasticsearch OOMs never had: a run does not fall off a cliff, it
 * spends a growing share of each interval in GC first (the 2h44m/32G death was measured thrashing at
 * 236/2048 free regions). Crossing {@value #GC_THRASH_WARN_FRACTION} emits a distinct WARN line so the
 * turn is visible in the log before the heap actually runs out. Heap is sampled <em>without</em> a
 * forced {@code System.gc()} on purpose — forcing GC every interval would itself perturb a
 * memory-constrained run; the used/committed/max triple plus the GC fraction tell the story unforced.
 *
 * <p><b>What to expect.</b> Throughput (elements/s over the run) and an ETA extrapolated from it, plus
 * the peak heap seen so far. ETA and percentage are shown only for the full first pass, where the
 * element total is the denominator; a worklist subset has no meaningful "total remaining".
 *
 * <p>A machine-readable {@code metrics.jsonl} (one flat JSON object per heartbeat) is appended next to
 * the run's checkpoint when a file is given, so the memory/progress curve can be plotted afterwards or
 * watched live. All IO here is best-effort: a metrics write that fails degrades the metrics, never the
 * analysis (and the caller's {@code feed(...)} guard swallows anything that escapes anyway).
 */
public class AnalysisProgressFeed implements AnalysisValueFeed {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisProgressFeed.class);

    private static final long DEFAULT_HEARTBEAT_MILLIS = 15_000;
    /** GC-time share of an interval above which we shout: the run is memory-bound and heading for trouble. */
    static final double GC_THRASH_WARN_FRACTION = 0.30;
    private static final long MB = 1024 * 1024;

    private final int totalElements;
    private final File metricsFile;
    private final long heartbeatMillis;

    private final long startMillis = System.currentTimeMillis();
    private long lastHeartbeatMillis = startMillis;
    private long lastGcCount = totalGcCount();
    private long lastGcTimeMillis = totalGcTimeMillis();
    private volatile int elementsDone;      // advanced on the coordinator thread, read by the sampler
    private volatile boolean firstPassOngoing = true;
    private volatile boolean running = true;
    private long peakUsedBytes;
    private Thread sampler;

    public AnalysisProgressFeed(int totalElements, File metricsFile) {
        this(totalElements, metricsFile, DEFAULT_HEARTBEAT_MILLIS);
    }

    public AnalysisProgressFeed(int totalElements, File metricsFile, long heartbeatMillis) {
        this.totalElements = totalElements;
        this.metricsFile = metricsFile;
        this.heartbeatMillis = heartbeatMillis;
        LOGGER.info("Progress feed: {} elements to analyze; heartbeat every {}s{}", totalElements,
                heartbeatMillis / 1000,
                metricsFile == null ? "" : ", metrics -> " + metricsFile);
        // A time-based daemon so the heap/GC curve is never blind: the wave-barrier events dry up inside
        // a giant-SCC first wave (server/main is essentially one wave — no barrier for minutes), which is
        // exactly when we most need to see whether the heap is climbing toward the ceiling. It samples on
        // the same throttle and yields to real wave/pass events (which reset the throttle).
        if (heartbeatMillis > 0) startSampler();
    }

    private void startSampler() {
        sampler = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(Math.min(heartbeatMillis, 2000));
                } catch (InterruptedException e) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (running && now - lastHeartbeatMillis >= heartbeatMillis) {
                    emit(now, "sampling", firstPassOngoing);
                }
            }
        }, "analysis-progress-sampler");
        sampler.setDaemon(true);
        sampler.start();
    }

    /** Stop the sampler; idempotent. Called on a terminal phase, safe to call more than once. */
    public void stop() {
        running = false;
        if (sampler != null) sampler.interrupt();
    }

    @Override
    public void waveCompleted(int iteration, int wave, Collection<Info> analyzed) {
        elementsDone += analyzed.size();
        long now = System.currentTimeMillis();
        if (now - lastHeartbeatMillis >= heartbeatMillis) {
            emit(now, "pass " + iteration + " wave " + wave, true);
        }
    }

    @Override
    public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
        // pass boundaries are infrequent (and the first one closes the multi-hour stretch): always emit.
        if (fullPass && iteration == 1) {
            elementsDone = totalElements; // waves should already sum to this
            firstPassOngoing = false;     // past pass 1, the ETA denominator no longer applies
        }
        emit(System.currentTimeMillis(), "pass " + iteration + " done"
                + (fullPass ? "" : " (worklist " + analyzed.size() + ")"), fullPass && iteration == 1);
    }

    @Override
    public void phase(Phase phase, int iteration) {
        if (phase.name().startsWith("TERMINAL")) stop(); // the analysis is done; no more sampling
        emit(System.currentTimeMillis(), phase + " @ pass " + iteration, false);
    }

    /**
     * @param showEta only meaningful while the full first pass is accumulating against {@code totalElements}
     */
    private synchronized void emit(long now, String where, boolean showEta) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long used = heap.getUsed();
        long committed = heap.getCommitted();
        long max = heap.getMax();
        if (used > peakUsedBytes) peakUsedBytes = used;

        long intervalMillis = Math.max(1, now - lastHeartbeatMillis);
        long gcCount = totalGcCount();
        long gcTime = totalGcTimeMillis();
        long gcCountDelta = gcCount - lastGcCount;
        long gcTimeDelta = gcTime - lastGcTimeMillis;
        double gcFraction = (double) gcTimeDelta / intervalMillis;
        lastGcCount = gcCount;
        lastGcTimeMillis = gcTime;
        lastHeartbeatMillis = now;

        long elapsedMillis = Math.max(1, now - startMillis);
        double rate = elementsDone * 1000.0 / elapsedMillis; // elements/s over the whole run
        long etaSec = showEta && rate > 0 && elementsDone < totalElements
                ? (long) ((totalElements - elementsDone) / rate) : -1;

        String pct = showEta && totalElements > 0
                ? String.format(" (%d%%)", 100 * elementsDone / totalElements) : "";
        String gcNote = gcFraction >= GC_THRASH_WARN_FRACTION
                ? String.format(" GC %.0f%% of interval!", gcFraction * 100)
                : (gcCountDelta > 0 ? String.format(" GC+%d/%dms", gcCountDelta, gcTimeDelta) : "");
        LOGGER.info("progress[{}]: {}/{} elem{} | heap {}/{}/{} | {} elem/s | ETA {} | peak {}{}",
                where, String.format("%,d", elementsDone), String.format("%,d", totalElements), pct,
                gb(used), gb(committed), gb(max), String.format("%.0f", rate),
                etaSec < 0 ? "-" : hms(etaSec * 1000), gb(peakUsedBytes), gcNote);

        if (gcFraction >= GC_THRASH_WARN_FRACTION) {
            LOGGER.warn("MEMORY PRESSURE: {}% of the last {}s spent in GC (heap {} of {} max, {} free) — "
                        + "the run is memory-bound and may not finish at this heap",
                    String.format("%.0f", gcFraction * 100), intervalMillis / 1000, gb(used), gb(max),
                    gb(max - used));
        }
        appendJsonl(now, where, used, committed, max, gcCountDelta, gcTimeDelta, gcFraction, rate, etaSec);
    }

    private void appendJsonl(long now, String where, long used, long committed, long max,
                             long gcCountDelta, long gcTimeDelta, double gcFraction, double rate, long etaSec) {
        if (metricsFile == null) return;
        String line = "{"
                + "\"t\":" + (now - startMillis)
                + ",\"where\":\"" + where + '"'
                + ",\"done\":" + elementsDone
                + ",\"total\":" + totalElements
                + ",\"heapUsedMB\":" + used / MB
                + ",\"heapCommittedMB\":" + committed / MB
                + ",\"heapMaxMB\":" + (max <= 0 ? -1 : max / MB)
                + ",\"peakUsedMB\":" + peakUsedBytes / MB
                + ",\"gcCountDelta\":" + gcCountDelta
                + ",\"gcTimeMsDelta\":" + gcTimeDelta
                + ",\"gcFraction\":" + String.format("%.3f", gcFraction)
                + ",\"rate\":" + String.format("%.1f", rate)
                + ",\"etaSec\":" + etaSec
                + "}\n";
        try {
            Files.writeString(metricsFile.toPath(), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.debug("metrics.jsonl append failed (degrading metrics, not analysis): {}", e.toString());
        }
    }

    public long peakUsedBytes() {
        return peakUsedBytes;
    }

    private static long totalGcCount() {
        long sum = 0;
        for (GarbageCollectorMXBean b : gcBeans()) {
            long c = b.getCollectionCount();
            if (c > 0) sum += c;
        }
        return sum;
    }

    private static long totalGcTimeMillis() {
        long sum = 0;
        for (GarbageCollectorMXBean b : gcBeans()) {
            long t = b.getCollectionTime();
            if (t > 0) sum += t;
        }
        return sum;
    }

    private static List<GarbageCollectorMXBean> gcBeans() {
        return ManagementFactory.getGarbageCollectorMXBeans();
    }

    private static String gb(long bytes) {
        if (bytes < 0) return "?";
        double g = bytes / (1024.0 * 1024 * 1024);
        return g >= 1.0 ? String.format("%.1fG", g) : (bytes / MB) + "M";
    }

    private static String hms(long millis) {
        long totalSec = millis / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        return h > 0 ? h + "h" + m + "m" : m + "m" + (totalSec % 60) + "s";
    }
}
