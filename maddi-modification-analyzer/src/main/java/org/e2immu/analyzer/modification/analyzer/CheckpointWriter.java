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

import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysisResults;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Checkpoint half of task #34 (checkpoint/resume v1): an {@link AnalysisValueFeed} that persists the
 * analyzed elements' values at every pass boundary, so a crashed multi-hour run (the elasticsearch
 * sweep takes 5h+) can be resumed instead of restarted.
 *
 * <p>Delta discipline: each pass writes ONLY the primary types containing elements analyzed in that
 * pass (the full set on the first pass, the dirty subset afterwards). File-per-primary-type naming
 * makes the deltas idempotent — a later pass simply overwrites the same file. A crash mid-write
 * corrupts at most one file; the restore side tolerates unparseable files.
 *
 * <p>Restore = {@code LoadAnalysisResults.goDir} over the checkpoint directory, then re-running the
 * iterating analyzer: preloaded values make the first pass a verify-certify sweep (TolerantWrite
 * makes re-writes of unchanged values cheap), which is also the soundness net — nothing is trusted
 * blindly. Source-change detection (per-sourceset fingerprints) is deliberately out of v1's scope:
 * resume assumes unchanged sources.
 *
 * <p>Exceptions thrown here are swallowed by the analyzer's feed guard: a checkpoint IO failure
 * degrades the checkpoint, never the analysis.
 */
public class CheckpointWriter implements AnalysisValueFeed {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckpointWriter.class);

    private final Runtime runtime;
    private final java.util.function.Supplier<Codec> codecSupplier;
    private final File directory;
    private final long waveFlushIntervalMillis;
    private int typesWritten;
    // wave-boundary protection for the multi-hour FIRST pass (see waveCompleted): primaries of
    // completed waves accumulate here and flush at most once per interval. Coordinator thread only.
    private final Set<TypeInfo> pendingWavePrimaries = new LinkedHashSet<>();
    private long lastWaveFlush = System.currentTimeMillis();

    private static final long DEFAULT_WAVE_FLUSH_INTERVAL_MILLIS = 60_000;

    /**
     * codecSupplier: a FRESH codec per type-write. A codec instance registers marker-variable
     * definitions once per instance; with per-type files, a shared instance puts the definition in
     * whichever file first used the marker, and every other file's reference then fails decode
     * ('Cannot find $_ce0M'). A fresh codec per file makes each file self-contained.
     */
    public CheckpointWriter(Runtime runtime, java.util.function.Supplier<Codec> codecSupplier, File directory) {
        this(runtime, codecSupplier, directory, DEFAULT_WAVE_FLUSH_INTERVAL_MILLIS);
    }

    public CheckpointWriter(Runtime runtime, java.util.function.Supplier<Codec> codecSupplier, File directory,
                            long waveFlushIntervalMillis) {
        this.runtime = runtime;
        this.codecSupplier = codecSupplier;
        this.directory = directory;
        this.waveFlushIntervalMillis = waveFlushIntervalMillis;
        if (directory.mkdirs()) {
            LOGGER.debug("Created checkpoint directory {}", directory);
        }
    }

    @Override
    public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
        // the pass write covers everything analyzed this pass, so pending wave primaries are subsumed
        pendingWavePrimaries.clear();
        lastWaveFlush = System.currentTimeMillis();
        Set<TypeInfo> primaries = primariesOf(analyzed);
        if (primaries.isEmpty()) return;
        write(primaries, "pass " + iteration);
    }

    /**
     * Intra-pass crash protection (the checkpoint granularity gap, 2026-07-19): pass-boundary writes
     * give NOTHING during a cold run's first pass, which at monorepo scale is the 3-4h+ stretch that
     * most needs protecting. Wave barriers are safe emission points; a time throttle batches the
     * (typically many, small) waves so a primary type whose elements span several waves is not
     * rewritten per wave. Values are provisional but monotone; the pass-boundary write supersedes.
     */
    @Override
    public void waveCompleted(int iteration, int wave, Collection<Info> analyzed) {
        pendingWavePrimaries.addAll(primariesOf(analyzed));
        long now = System.currentTimeMillis();
        if (pendingWavePrimaries.isEmpty() || now - lastWaveFlush < waveFlushIntervalMillis) return;
        Set<TypeInfo> toWrite = new LinkedHashSet<>(pendingWavePrimaries);
        pendingWavePrimaries.clear();
        lastWaveFlush = now;
        write(toWrite, "wave " + wave + " (pass " + iteration + ")");
    }

    private static Set<TypeInfo> primariesOf(Collection<Info> analyzed) {
        Set<TypeInfo> primaries = new LinkedHashSet<>();
        for (Info info : analyzed) {
            TypeInfo primary = info.typeInfo() == null ? null : info.typeInfo().primaryType();
            if (primary != null) primaries.add(primary);
        }
        return primaries;
    }

    private void write(Set<TypeInfo> primaries, String label) {
        // write PER TYPE so one unencodable type (mid-iteration values can trip codec asserts) costs
        // only itself, not the whole pass; and via a temp dir + atomic move, so a mid-encode crash
        // never leaves a truncated file in the checkpoint. Missing types are re-analyzed on resume.
        int ok = 0, failed = 0;
        for (TypeInfo primary : primaries) {
            Trie<TypeInfo> trie = new Trie<>();
            trie.add(primary.fullyQualifiedName().split("\\."), primary);
            java.nio.file.Path tmpDir = null;
            try {
                tmpDir = Files.createTempDirectory(directory.toPath(), ".w");
                new WriteAnalysisResults(runtime).write(tmpDir.toFile(), trie, codecSupplier.get());
                try (var files = Files.walk(tmpDir).filter(p -> p.toString().endsWith(".json"))) {
                    for (java.nio.file.Path p : files.toList()) {
                        Files.move(p, directory.toPath().resolve(p.getFileName()),
                                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                ok++;
            } catch (IOException | RuntimeException | AssertionError e) {
                failed++;
                LOGGER.debug("Checkpoint skip {} after {}: {}", primary, label, e.toString());
            } finally {
                if (tmpDir != null) {
                    try (var leftovers = Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder())) {
                        for (java.nio.file.Path p : leftovers.toList()) Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // temp-dir cleanup is best-effort
                    }
                }
            }
        }
        typesWritten += ok;
        LOGGER.info("Checkpoint after {}: wrote {} primary type(s) ({} skipped), {} cumulative",
                label, ok, failed, typesWritten);
    }

    @Override
    public void phase(Phase phase, int iteration) {
        if (phase.name().startsWith("TERMINAL")) {
            try {
                Files.writeString(new File(directory, "checkpoint-terminal.txt").toPath(),
                        phase + " at iteration " + iteration + "\n");
            } catch (IOException e) {
                LOGGER.error("Cannot write terminal marker: {}", e.toString());
            }
        }
    }

    public int typesWritten() {
        return typesWritten;
    }
}
