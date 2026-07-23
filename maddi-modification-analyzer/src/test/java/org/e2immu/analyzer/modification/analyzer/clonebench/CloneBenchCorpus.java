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

package org.e2immu.analyzer.modification.analyzer.clonebench;

import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The clone-bench corpus root, resolved exactly as {@code TestOssCorpus} resolves the test-oss root, so the two
 * large corpora are configured the same way:
 * <ol>
 *   <li>system property {@code -Dtestarchive.root=...}</li>
 *   <li>environment variable {@code TESTARCHIVE_ROOT}</li>
 *   <li>default {@code ../../testarchive}: a sibling of the repository checkout</li>
 * </ol>
 * The default only holds when the checkout sits directly under a workspace that also contains the corpus; a
 * worktree one level deeper (or one whose setup script did not link the corpus in) needs the override.
 * <p>
 * Use {@link #assumeAvailable()} rather than an assertion. An absent corpus must be indistinguishable from a
 * deliberate skip, never from a regression -- and, just as importantly, never from a pass: TestCloneBench used
 * to iterate an empty directory list and report success having analyzed 0 types, which is the one outcome that
 * makes a proving ground actively misleading.
 */
public class CloneBenchCorpus {

    public static final Path ROOT = resolveRoot();

    private static Path resolveRoot() {
        String p = System.getProperty("testarchive.root");
        if (p == null || p.isBlank()) p = System.getenv("TESTARCHIVE_ROOT");
        return p == null || p.isBlank() ? Path.of("../../testarchive") : Path.of(p);
    }

    /** The source directory of one clone-bench project, e.g. {@code <root>/switch_pure_compiles/src/main/java}. */
    public static Path sourceDirectory(String project) {
        return ROOT.resolve(project).resolve("src/main/java");
    }

    public static void assumeAvailable() {
        Assumptions.assumeTrue(Files.isDirectory(ROOT),
                "requires the clone-bench corpus ('analyzed' branch of testarchive) at "
                + ROOT.toAbsolutePath().normalize()
                + "; override with -Dtestarchive.root=... or TESTARCHIVE_ROOT");
    }
}
