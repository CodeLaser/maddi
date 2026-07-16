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

package org.e2immu.analyzer.run.openjdkmain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code rewire-tests} end to end: RunRewireTests edits a source file on disk, asks the inspector what changed,
 * re-parses with the changed type INVALID and its dependents REWIRE, and restores the file — over and over.
 */
public class TestRewireTests {

    private static final Map<String, String> MAIN = Map.of(
            "p/Base.java", """
                    package p;
                    public class Base {
                        public String name() { return "base"; }
                    }
                    """,
            "p/Mid.java", """
                    package p;
                    public class Mid {
                        private final Base base = new Base();
                        public String name() { return base.name(); }
                    }
                    """,
            "p/Top.java", """
                    package p;
                    public class Top {
                        private final Mid mid = new Mid();
                        public String describe() { return mid.name(); }
                    }
                    """);

    // a second source set, which depends on the first: a change in main must have these REWIRED, not re-scanned
    private static final Map<String, String> TEST = Map.of(
            "q/UsesTop.java", """
                    package q;
                    import p.Top;
                    public class UsesTop {
                        public String go() { return new Top().describe(); }
                    }
                    """);

    @Test
    public void rewireTests(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Path test = tmp.resolve("test");
        for (Map.Entry<String, String> e : MAIN.entrySet()) {
            Path p = src.resolve(e.getKey());
            Files.createDirectories(p.getParent());
            Files.writeString(p, e.getValue());
        }
        for (Map.Entry<String, String> e : TEST.entrySet()) {
            Path p = test.resolve(e.getKey());
            Files.createDirectories(p.getParent());
            Files.writeString(p, e.getValue());
        }

        int exit = Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.TEST_SOURCE, test.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_REWIRE_TESTS});

        assertEquals(Main.EXIT_OK, exit, "the rewire test loop must complete cleanly");

        // every file must be back exactly as it was: the loop edits and restores
        for (Map.Entry<String, String> e : MAIN.entrySet()) {
            assertEquals(e.getValue(), Files.readString(src.resolve(e.getKey())), e.getKey() + " must be restored");
        }
        for (Map.Entry<String, String> e : TEST.entrySet()) {
            assertEquals(e.getValue(), Files.readString(test.resolve(e.getKey())), e.getKey() + " must be restored");
        }
    }
}
