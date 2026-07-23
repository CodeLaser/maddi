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
import org.e2immu.analyzer.modification.analyzer.impl.StaticSideEffectAnalyzerImpl;
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
 * The confinement guard, field-granularity separation arm (road-to-immutability 050, "Ignoring modifications as
 * manual hidden content"). {@code @IgnoreModifications} is sound manual hidden content only when the ignored
 * field is independent of the type's accessible content: a modification reached through the ignored stratum must
 * not touch accessible state. {@code guardIgnoreModificationsSeparation} warns (never caps) when the ignored
 * field shares content with an accessible field of the same type.
 * <p>
 * Coverage is deliberately conservative, mirroring {@code guardDynamicImmutableFields}: it reports a
 * non-decoration link to a NAMED accessible field of the same primary type. Referencing accessible content from
 * the stratum without sharing (the analysis overlay's normal use -- it stores immutable {@code Value} objects)
 * produces no such link and stays silent. The global-escape/containment arm ({@code @StaticSideEffects}) is a
 * separate, later piece.
 */
public class TestGuardIgnoreModifications extends CommonTest {

    private record Run(List<Message> messages, TypeInfo typeInfo) {
        List<Message> notConfined() {
            return messages.stream()
                    .filter(m -> GuardAnalyzerImpl.IGNORE_MODIFICATIONS_NOT_CONFINED.equals(m.category())).toList();
        }
    }

    private Run analyzeWithGuard(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true).build());
        analyzer.analyze(analysisOrder);
        return new Run(analyzer.messages(), typeInfo);
    }

    /**
     * The well-behaved shape, exactly like an analysis overlay: a fresh, independent {@code @IgnoreModifications}
     * store that only ever accumulates, and shares nothing with the accessible field {@code data}.
     */
    @Language("java")
    private static final String CONFINED = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.annotation.rare.IgnoreModifications;

            public class X {
                private final List<String> data = new ArrayList<>();
                @IgnoreModifications
                private final List<String> log = new ArrayList<>();

                public void add(String s) {
                    data.add(s);
                    log.add("added " + s);
                }

                public List<String> data() {
                    return data;
                }
            }
            """;

    @DisplayName("a fresh, independent ignore-mod store shares nothing accessible: silent")
    @Test
    public void testConfinedIsSilent() throws IOException {
        Run run = analyzeWithGuard("a.b.X", CONFINED);
        assertTrue(run.notConfined().isEmpty(),
                "a confined @IgnoreModifications store must not be flagged, have: "
                + run.notConfined().stream().map(Message::message).toList());
    }

    /**
     * A separation violation: the {@code @IgnoreModifications} field {@code mirror} is made to hold the very same
     * mutable elements as the accessible field {@code data}. A modification reached through the disclaimed
     * {@code mirror} (mutating a shared {@code StringBuilder}) is observable through {@code data}, so the
     * disclaimer is not confined to the ignored stratum.
     */
    @Language("java")
    private static final String NOT_CONFINED = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.annotation.rare.IgnoreModifications;

            public class X {
                private final List<StringBuilder> data = new ArrayList<>();
                @IgnoreModifications
                private final List<StringBuilder> mirror = new ArrayList<>();

                public void take(StringBuilder sb) {
                    data.add(sb);
                    mirror.add(sb);
                }

                public List<StringBuilder> data() {
                    return data;
                }
            }
            """;

    @DisplayName("an ignore-mod field sharing mutable content with an accessible field: warned, not silent")
    @Test
    public void testNotConfinedIsWarned() throws IOException {
        Run run = analyzeWithGuard("a.b.X", NOT_CONFINED);
        // observed empirically; if the link analysis does not surface the field-to-field share, this documents
        // the guard's (conservative) reach rather than asserting coverage it does not have.
        System.out.println("NOT_CONFINED findings: " + run.notConfined().stream().map(Message::message).toList());
        assertEquals(1, run.notConfined().size(),
                "the shared-content ignore-mod field must be flagged, have: "
                + run.notConfined().stream().map(Message::message).toList());
        assertTrue(run.notConfined().getFirst().message().contains("mirror"),
                run.notConfined().getFirst().message());
    }

    /**
     * The global-escape arm: a modifying call on the ignored field whose callee has a static side effect
     * (it modifies another type's static state) leaves the ignored stratum. {@code Sink.record()} bumps a static
     * counter of another type, so calling it on the {@code @IgnoreModifications} field {@code sink} is not
     * confined; {@code sink.plain()} is.
     */
    @Language("java")
    private static final String GLOBAL_ESCAPE = """
            package a.b;
            import org.e2immu.annotation.rare.IgnoreModifications;
            class Global { static int count; }
            public class X {
                static class Sink {
                    void record() { Global.count = Global.count + 1; }
                    void plain() { }
                }
                @IgnoreModifications
                private final Sink sink = new Sink();
                public void bad() { sink.record(); }
                public void ok() { sink.plain(); }
            }
            """;

    @DisplayName("global-escape arm: a static-side-effect call on an ignore-mod field is not confined")
    @Test
    public void testGlobalEscapeIsWarned() throws IOException {
        boolean saved = StaticSideEffectAnalyzerImpl.ENABLED;
        StaticSideEffectAnalyzerImpl.ENABLED = true;
        try {
            Run run = analyzeWithGuard("a.b.X", GLOBAL_ESCAPE);
            List<Message> notConfined = run.notConfined();
            assertEquals(1, notConfined.size(),
                    "the static-side-effect call on the ignore-mod field must be flagged, have: "
                    + notConfined.stream().map(Message::message).toList());
            String msg = notConfined.getFirst().message();
            assertTrue(msg.contains("record") && msg.contains("static side effect") && msg.contains("sink"), msg);
        } finally {
            StaticSideEffectAnalyzerImpl.ENABLED = saved;
        }
    }

    /**
     * The AAPI safe-surface path, end to end: the escape is not a visible static-field write but a call to a
     * method contracted {@code @StaticSideEffects} — the stand-in for {@code System.setOut}, whose global effect
     * is invisible from source. {@code Sink.reconfigure()} calls the contracted {@code Native.install()}, so the
     * analyzer propagates the static side effect onto {@code reconfigure()}, and calling it on the
     * {@code @IgnoreModifications} field {@code sink} leaves the ignored stratum.
     */
    @Language("java")
    private static final String GLOBAL_ESCAPE_VIA_CONTRACT = """
            package a.b;
            import org.e2immu.annotation.rare.IgnoreModifications;
            import org.e2immu.annotation.rare.StaticSideEffects;
            public class X {
                static class Native {
                    @StaticSideEffects void install() { }   // effect invisible from body, asserted by contract
                }
                static class Sink {
                    private final Native n = new Native();
                    void reconfigure() { n.install(); }     // inherits the static side effect by propagation
                    void plain() { }
                }
                @IgnoreModifications
                private final Sink sink = new Sink();
                public void bad() { sink.reconfigure(); }
                public void ok() { sink.plain(); }
            }
            """;

    @DisplayName("global-escape via a contracted @StaticSideEffects callee: propagates, then the guard fires")
    @Test
    public void testGlobalEscapeViaContractIsWarned() throws IOException {
        boolean saved = StaticSideEffectAnalyzerImpl.ENABLED;
        StaticSideEffectAnalyzerImpl.ENABLED = true;
        try {
            Run run = analyzeWithGuard("a.b.X", GLOBAL_ESCAPE_VIA_CONTRACT);
            List<Message> notConfined = run.notConfined();
            assertEquals(1, notConfined.size(),
                    "the propagated static-side-effect call on the ignore-mod field must be flagged, have: "
                    + notConfined.stream().map(Message::message).toList());
            String msg = notConfined.getFirst().message();
            assertTrue(msg.contains("reconfigure") && msg.contains("static side effect") && msg.contains("sink"), msg);
        } finally {
            StaticSideEffectAnalyzerImpl.ENABLED = saved;
        }
    }
}
