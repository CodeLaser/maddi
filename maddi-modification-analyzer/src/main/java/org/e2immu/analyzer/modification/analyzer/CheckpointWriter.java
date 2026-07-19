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
    private final Codec codec;
    private final File directory;
    private int typesWritten;

    public CheckpointWriter(Runtime runtime, Codec codec, File directory) {
        this.runtime = runtime;
        this.codec = codec;
        this.directory = directory;
        if (directory.mkdirs()) {
            LOGGER.debug("Created checkpoint directory {}", directory);
        }
    }

    @Override
    public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
        Set<TypeInfo> primaries = new LinkedHashSet<>();
        for (Info info : analyzed) {
            TypeInfo primary = info.typeInfo() == null ? null : info.typeInfo().primaryType();
            if (primary != null) primaries.add(primary);
        }
        if (primaries.isEmpty()) return;
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
                new WriteAnalysisResults(runtime).write(tmpDir.toFile(), trie, codec);
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
                LOGGER.debug("Checkpoint skip {} after pass {}: {}", primary, iteration, e.toString());
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
        LOGGER.info("Checkpoint after pass {}: wrote {} primary type(s) ({} skipped), {} cumulative",
                iteration, ok, failed, typesWritten);
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
