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
 * Method-level near-misses: an abstract method one implementation short of being contractable {@code @NotModified}
 * (no implementation modifies its receiver) or {@code @Independent} (no implementation exposes state). The mirror
 * image of the guard's {@code guardMethod}, driven off the implementations' computed values and naming the single
 * culprit ("1 of N"). The strict default policy (minImplementations=3, maxBlockingImplementations=1) applies.
 */
public class TestNearMissMethod extends CommonTest {

    private List<Message> analyze(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setMaxIterations(10).setGuardContracts(false).setWarnNearMisses(true).build());
        analyzer.analyze(analysisOrder);
        return analyzer.messages();
    }

    private static Message finding(List<Message> messages, String category, String infoFqn) {
        return messages.stream()
                .filter(m -> category.equals(m.category()) && infoFqn.equals(m.info().fullyQualifiedName()))
                .findFirst().orElse(null);
    }

    // ---- @NotModified: one modifying implementation out of three ----

    @Language("java")
    private static final String NOT_MODIFIED = """
            package a.b;
            public class M {
                interface Task {
                    void run();
                }
                static class Clean1 implements Task { public void run() {} }
                static class Clean2 implements Task { public void run() {} }
                static class Dirty implements Task {
                    private int c;
                    public void run() { c++; } // modifies the receiver: the only blocking implementation
                }
            }
            """;

    @DisplayName("an abstract method blocked by one modifying implementation: @NotModified near-miss, with blame")
    @Test
    public void testNotModifiedOneOfThree() throws IOException {
        List<Message> messages = analyze("a.b.M", NOT_MODIFIED);
        Message m = finding(messages, GuardAnalyzerImpl.NEAR_MISS_NOT_MODIFIED, "a.b.M.Task.run()");
        assertNotNull(m, () -> "expected a @NotModified near-miss on Task.run(), have: "
                               + messages.stream().filter(x -> x.category().startsWith("near-miss"))
                                       .map(x -> x.category() + ":" + x.info().fullyQualifiedName()).toList());
        assertTrue(m.level().isWarning());
        assertTrue(m.message().contains("would satisfy @NotModified"), m.message());
        assertTrue(m.message().contains("1 of its 3 implementations"), m.message());
        Message attribution = m.causes().getFirst();
        assertTrue(attribution.message().contains("a.b.M.Dirty.run()"), attribution.message());
        assertTrue(attribution.message().contains("the only 1 of 3 implementations"), attribution.message());
        Message blame = attribution.causes().getFirst();
        assertTrue(blame.message().contains("assigns field 'c'"), blame.message());
        assertNotNull(blame.source());
    }

    @DisplayName("two modifying implementations: not a @NotModified near-miss")
    @Test
    public void testNotModifiedTwoOfThreeSuppressed() throws IOException {
        @Language("java") String twoDirty = """
                package a.b;
                public class M {
                    interface Task { void run(); }
                    static class Clean implements Task { public void run() {} }
                    static class Dirty1 implements Task { private int c; public void run() { c++; } }
                    static class Dirty2 implements Task { private int d; public void run() { d++; } }
                }
                """;
        List<Message> messages = analyze("a.b.M", twoDirty);
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_NOT_MODIFIED, "a.b.M.Task.run()"));
    }

    @DisplayName("only two implementations: not a @NotModified near-miss (below minImplementations)")
    @Test
    public void testNotModifiedTooFewImplementations() throws IOException {
        @Language("java") String two = """
                package a.b;
                public class M {
                    interface Task { void run(); }
                    static class Clean implements Task { public void run() {} }
                    static class Dirty implements Task { private int c; public void run() { c++; } }
                }
                """;
        List<Message> messages = analyze("a.b.M", two);
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_NOT_MODIFIED, "a.b.M.Task.run()"));
    }

    // ---- @Independent: one exposing implementation out of three ----

    @Language("java")
    private static final String INDEPENDENT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class N {
                interface Source {
                    List<String> get();
                }
                static class Copy1 implements Source {
                    private final List<String> data = new ArrayList<>();
                    public List<String> get() { return List.copyOf(data); }
                }
                static class Copy2 implements Source {
                    private final List<String> data = new ArrayList<>();
                    public List<String> get() { return List.copyOf(data); }
                }
                static class Expose implements Source {
                    private final List<String> data = new ArrayList<>();
                    public List<String> get() { return data; } // exposes the field: the only dependent implementation
                }
            }
            """;

    @DisplayName("an abstract method blocked by one exposing implementation: @Independent near-miss, with blame")
    @Test
    public void testIndependentOneOfThree() throws IOException {
        List<Message> messages = analyze("a.b.N", INDEPENDENT);
        Message m = finding(messages, GuardAnalyzerImpl.NEAR_MISS_INDEPENDENT, "a.b.N.Source.get()");
        assertNotNull(m, () -> "expected an @Independent near-miss on Source.get(), have: "
                               + messages.stream().filter(x -> x.category().startsWith("near-miss"))
                                       .map(x -> x.category() + ":" + x.info().fullyQualifiedName()).toList());
        assertTrue(m.level().isWarning());
        assertTrue(m.message().contains("would satisfy @Independent"), m.message());
        assertTrue(m.message().contains("1 of its 3 implementations"), m.message());
        Message attribution = m.causes().getFirst();
        assertTrue(attribution.message().contains("a.b.N.Expose.get()"), attribution.message());
        assertTrue(attribution.message().contains("the only 1 of 3 implementations"), attribution.message());
        Message blame = attribution.causes().getFirst();
        assertTrue(blame.message().contains("'data'"), blame.message());
    }

    @DisplayName("no exposing implementation: no @Independent near-miss (already qualifies)")
    @Test
    public void testIndependentAllCleanSilent() throws IOException {
        @Language("java") String allCopy = """
                package a.b;
                import java.util.ArrayList;
                import java.util.List;
                public class N {
                    interface Source { List<String> get(); }
                    static class Copy1 implements Source {
                        private final List<String> data = new ArrayList<>();
                        public List<String> get() { return List.copyOf(data); }
                    }
                    static class Copy2 implements Source {
                        private final List<String> data = new ArrayList<>();
                        public List<String> get() { return List.copyOf(data); }
                    }
                    static class Copy3 implements Source {
                        private final List<String> data = new ArrayList<>();
                        public List<String> get() { return List.copyOf(data); }
                    }
                }
                """;
        List<Message> messages = analyze("a.b.N", allCopy);
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_INDEPENDENT, "a.b.N.Source.get()"),
                "all implementations are independent, so nothing to suggest");
    }
}
