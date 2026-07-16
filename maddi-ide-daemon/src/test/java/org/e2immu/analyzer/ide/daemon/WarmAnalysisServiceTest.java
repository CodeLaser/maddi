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

package org.e2immu.analyzer.ide.daemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.analyze;
import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.displayFor;
import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.finding;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture-driven suite over the real analysis pipeline. Each test writes a small Java snippet, analyzes it
 * in the warm service, and asserts the plain-JSON result. Add a case by adding a snippet + a few assertions.
 */
public class WarmAnalysisServiceTest {

    /** A @Container implementation that modifies its argument: violation + why-chain, @Container, @Modified param. */
    @Test
    public void containerViolationIsFoundAndExplained(@TempDir Path projectDir) throws Exception {
        DaemonProtocol.Result r = analyze(projectDir, "x/Example.java", """
                package x;
                import org.e2immu.annotation.Container;

                class Mutable {
                    private int value;
                    void set(int v) { this.value = v; } // modifying: assigns a field
                    int get() { return value; }
                }

                @Container
                interface HasAdd {
                    void add(Mutable m);
                }

                class BadImpl implements HasAdd {
                    @Override
                    public void add(Mutable m) { m.set(3); } // modifies the argument → violates @Container
                }
                """);

        assertEquals(0, r.parseErrorCount(), "unexpected parse errors");

        DaemonProtocol.Finding violation = finding(r, "contract-violation").orElse(null);
        assertTrue(violation != null, "expected a contract-violation");
        assertEquals("ERROR", violation.severity());
        assertTrue(violation.uri() != null && violation.beginLine() != null, "violation should be located");
        assertFalse(violation.causes() == null || violation.causes().isEmpty(), "violation should carry a why-chain");

        // the contracted interface is immutable + container, rendered @ImmutableContainer
        assertTrue(displayFor(r, "TYPE", "HasAdd").stream().anyMatch(a -> a.contains("Container")),
                "the contracted interface should show a container annotation");
        assertTrue(displayFor(r, "PARAMETER", "add").contains("@Modified"),
                "the modified argument (BadImpl.add) should be rendered @Modified");
    }

    /** JDK hints are preloaded and make library-dependent analysis correct (List.size() is @NotModified). */
    @Test
    public void jdkHintsMakeReadOnlyMethodNotModified(@TempDir Path projectDir) throws Exception {
        DaemonProtocol.Result r = analyze(projectDir, "x/Holder.java", """
                package x;
                public class Holder {
                    private final java.util.List<String> items = new java.util.ArrayList<>();
                    int count() { return items.size(); } // only reads → @NotModified iff List.size() hints load
                }
                """);
        assertEquals(0, r.parseErrorCount(), "unexpected parse errors");
        assertTrue(r.hintsLoaded() > 0, "expected JDK/library analysis hints to be preloaded");
        assertTrue(displayFor(r, "METHOD", "count").contains("@NotModified"),
                "count() should be @NotModified (needs java.util.List.size() hints)");
    }

    /** A class whose non-private methods do not modify their arguments is a @Container. */
    @Test
    public void containerClassIsRecognised(@TempDir Path projectDir) throws Exception {
        DaemonProtocol.Result r = analyze(projectDir, "x/Box.java", """
                package x;
                public class Box {
                    private int value;
                    public int get() { return value; }
                    public void set(int v) { this.value = v; } // param is an int → not modifiable
                }
                """);
        assertEquals(0, r.parseErrorCount(), "unexpected parse errors");
        assertTrue(displayFor(r, "TYPE", "Box").contains("@Container"),
                "Box has no modifiable parameters, so it is a @Container");
        assertFalse(displayFor(r, "TYPE", "Box").stream().anyMatch(a -> a.contains("@Immutable")),
                "Box has a mutable field, so it is not immutable");
    }
}
