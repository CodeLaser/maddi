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

package org.e2immu.analyzer.modification.analyzer.guard;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer.NearMissPolicy;
import org.e2immu.analyzer.modification.analyzer.impl.GuardAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Near-miss warnings: the mirror image of the guard. No {@code @Container} is written, the computed
 * {@code CONTAINER_TYPE} is decided FALSE, but the type is one culprit short of being a container — a WARN
 * (category {@code near-miss-container}) suggests the annotation and names the blocking parameter, and, when the
 * blocker sits on an abstract method, the single implementation responsible ("1 of N").
 */
public class TestNearMissContainer extends CommonTest {

    private List<Message> analyze(String fqn, String source, boolean nearMisses, NearMissPolicy policy)
            throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzerImpl.ConfigurationBuilder cb = new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(10).setGuardContracts(false).setWarnNearMisses(nearMisses);
        if (policy != null) cb.setNearMissPolicy(policy);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(), cb.build());
        analyzer.analyze(analysisOrder);
        return analyzer.messages();
    }

    private static List<Message> nearMisses(List<Message> messages) {
        return messages.stream().filter(m -> GuardAnalyzerImpl.NEAR_MISS_CONTAINER.equals(m.category())).toList();
    }

    // ---- concrete surface: one modifying method out of a surface of eight, under the strict default policy ----

    @Language("java")
    private static final String CONCRETE_SURFACE = """
            package a.b;
            public class X {
                public int read0(StringBuilder sb) { return sb.length(); }
                public int read1(StringBuilder sb) { return sb.length(); }
                public int read2(StringBuilder sb) { return sb.length(); }
                public int read3(StringBuilder sb) { return sb.length(); }
                public int read4(StringBuilder sb) { return sb.length(); }
                public int read5(StringBuilder sb) { return sb.length(); }
                public int read6(StringBuilder sb) { return sb.length(); }
                public void bang(StringBuilder sb) { sb.append('!'); } // the only modifier: blocks @Container
            }
            """;

    @DisplayName("a class one modifying parameter short of @Container is reported, with the blame")
    @Test
    public void testConcreteSurface() throws IOException {
        List<Message> nm = nearMisses(analyze("a.b.X", CONCRETE_SURFACE, true, null));
        assertEquals(1, nm.size(), () -> "expected one near-miss, have: " + nm.stream().map(Message::message).toList());
        Message m = nm.getFirst();
        assertTrue(m.level().isWarning());
        assertEquals("a.b.X", m.info().fullyQualifiedName());
        assertTrue(m.message().contains("would satisfy @Container"), m.message());
        assertTrue(m.message().contains("1 of its 8 parameter slots"), m.message());
        assertTrue(m.message().contains("parameter 'sb' of a.b.X.bang"), m.message());
        // the single cause is the direct-site blame: sb.append(...) modifies sb
        assertEquals(1, m.causes().size());
        Message blame = m.causes().getFirst();
        assertTrue(blame.message().contains("sb.append(...)") && blame.message().contains("modifies 'sb'"),
                blame.message());
        assertNotNull(blame.source(), "the blame must point at the modifying statement (line/col)");
    }

    @DisplayName("below the surface floor (only three slots): no near-miss under the strict default")
    @Test
    public void testBelowSurfaceFloor() throws IOException {
        @Language("java") String small = """
                package a.b;
                public class X {
                    public int read0(StringBuilder sb) { return sb.length(); }
                    public int read1(StringBuilder sb) { return sb.length(); }
                    public void bang(StringBuilder sb) { sb.append('!'); }
                }
                """;
        assertTrue(nearMisses(analyze("a.b.X", small, true, null)).isEmpty(),
                "three slots is below minParameterSlots=7, so not a near-miss");
    }

    @DisplayName("guard/near-miss disabled: the same class yields no near-miss findings")
    @Test
    public void testDisabled() throws IOException {
        assertTrue(nearMisses(analyze("a.b.X", CONCRETE_SURFACE, false, null)).isEmpty());
    }

    @DisplayName("a genuine container is silent (positive control)")
    @Test
    public void testGenuineContainer() throws IOException {
        @Language("java") String container = """
                package a.b;
                public class C {
                    public int r0(StringBuilder sb) { return sb.length(); }
                    public int r1(StringBuilder sb) { return sb.length(); }
                    public int r2(StringBuilder sb) { return sb.length(); }
                    public int r3(StringBuilder sb) { return sb.length(); }
                    public int r4(StringBuilder sb) { return sb.length(); }
                    public int r5(StringBuilder sb) { return sb.length(); }
                    public int r6(StringBuilder sb) { return sb.length(); }
                    public int r7(StringBuilder sb) { return sb.length(); }
                }
                """;
        assertTrue(nearMisses(analyze("a.b.C", container, true, null)).isEmpty(),
                "a real container has no blocking slot, so nothing to suggest");
    }

    // ---- abstract "1 of N": an interface blocked by a single modifying implementation out of many ----
    // A realistic interface rarely carries seven parameter slots of its own, so these scenarios use a policy with
    // minParameterSlots=1 to exercise the implementation-attribution gate directly (minImplementations/max stay
    // at the strict defaults). The interface's own container value already folds every implementation.

    private static final NearMissPolicy SINGLE_SLOT = new NearMissPolicy(1, 1, 3, 1);

    private static String sink(String clean1Body, String clean2Body, String badBody, boolean includeClean2) {
        return "package a.b;\n"
               + "public class Y {\n"
               + "    interface Sink { void accept(StringBuilder sb); }\n"
               + "    static class Impl1 implements Sink { public void accept(StringBuilder sb) { " + clean1Body + " } }\n"
               + (includeClean2
                       ? "    static class Impl2 implements Sink { public void accept(StringBuilder sb) { " + clean2Body + " } }\n"
                       : "")
               + "    static class Impl3 implements Sink { public void accept(StringBuilder sb) { " + badBody + " } }\n"
               + "}\n";
    }

    private static Message sinkFinding(List<Message> nm) {
        return nm.stream().filter(m -> "a.b.Y.Sink".equals(m.info().fullyQualifiedName())).findFirst().orElse(null);
    }

    @DisplayName("an interface blocked by exactly one of three implementations: near-miss naming that implementation")
    @Test
    public void testAbstractOneOfThree() throws IOException {
        String source = sink("sb.length();", "sb.length();", "sb.append('!');", true);
        List<Message> nm = nearMisses(analyze("a.b.Y", source, true, SINGLE_SLOT));
        Message sink = sinkFinding(nm);
        assertNotNull(sink, () -> "expected a near-miss on the interface, have: "
                                  + nm.stream().map(m -> m.info().fullyQualifiedName()).toList());
        assertTrue(sink.level().isWarning());
        assertTrue(sink.message().contains("would satisfy @Container"), sink.message());
        assertEquals(1, sink.causes().size());
        Message attribution = sink.causes().getFirst();
        assertTrue(attribution.message().contains("a.b.Y.Impl3.accept"), attribution.message());
        assertTrue(attribution.message().contains("the only 1 of 3 implementations"), attribution.message());
        // the attribution nests the direct-site blame on the culprit implementation
        assertEquals(1, attribution.causes().size());
        Message blame = attribution.causes().getFirst();
        assertTrue(blame.message().contains("sb.append(...)") && blame.message().contains("modifies 'sb'"),
                blame.message());
    }

    @DisplayName("two of three implementations modify: the interface is not a near-miss (above maxBlockingImplementations)")
    @Test
    public void testAbstractTwoOfThreeSuppressed() throws IOException {
        String source = sink("sb.append('?');", "sb.length();", "sb.append('!');", true);
        List<Message> nm = nearMisses(analyze("a.b.Y", source, true, SINGLE_SLOT));
        assertNull(sinkFinding(nm), "two modifying implementations is not 'one culprit'; the interface must be silent");
    }

    @DisplayName("only two implementations: the interface is not a near-miss (below minImplementations)")
    @Test
    public void testAbstractTooFewImplementations() throws IOException {
        String source = sink("sb.length();", "", "sb.append('!');", false);
        List<Message> nm = nearMisses(analyze("a.b.Y", source, true, SINGLE_SLOT));
        assertNull(sinkFinding(nm), "with only two implementations the '1 of N' story is not compelling");
    }
}
