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
 * Type-level immutability near-misses: a class one field short of {@code @Immutable} or {@code @Independent}. The
 * mirror image of {@code guardImmutable} / {@code guardIndependentType}, driven off the per-field computed values.
 * {@code @Immutable} subsumes {@code @Independent} (its rule 3), so an immutable near-miss suppresses the
 * independence one for the same type.
 */
public class TestNearMissType extends CommonTest {

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

    // ---- @Immutable near-miss: one non-final field (rule 0) among otherwise-immutable fields ----

    @Language("java")
    private static final String IMMUTABLE_ONE_FIELD = """
            package a.b;
            public class P {
                private final int a;
                private final String b;
                private int c; // non-final (setC below): the only blocking field, rule 0
                P(int a, String b, int c) { this.a = a; this.b = b; this.c = c; }
                public int a() { return a; }
                public String b() { return b; }
                public int c() { return c; }
                public void setC(int c) { this.c = c; }
            }
            """;

    @DisplayName("a class one non-final field short of @Immutable is reported, naming the field and the rule")
    @Test
    public void testImmutableOneField() throws IOException {
        List<Message> messages = analyze("a.b.P", IMMUTABLE_ONE_FIELD);
        Message m = finding(messages, GuardAnalyzerImpl.NEAR_MISS_IMMUTABLE, "a.b.P");
        assertNotNull(m, () -> "expected a @Immutable near-miss on P, have: "
                               + messages.stream().filter(x -> x.category().startsWith("near-miss"))
                                       .map(x -> x.category() + ":" + x.info().fullyQualifiedName()).toList());
        assertTrue(m.level().isWarning());
        assertTrue(m.message().contains("would satisfy @Immutable"), m.message());
        assertTrue(m.message().contains("1 of its 3 fields"), m.message());
        Message cause = m.causes().getFirst();
        assertTrue(cause.message().contains("field 'c'"), cause.message());
        assertTrue(cause.message().contains("rule 0"), cause.message());
    }

    @DisplayName("two non-final fields: not an @Immutable near-miss (above maxBlockingSlots)")
    @Test
    public void testTwoBlockingFieldsSuppressed() throws IOException {
        @Language("java") String twoBad = """
                package a.b;
                public class P {
                    private final int a;
                    private int b;
                    private int c;
                    P(int a, int b, int c) { this.a = a; this.b = b; this.c = c; }
                    public void setB(int b) { this.b = b; }
                    public void setC(int c) { this.c = c; }
                }
                """;
        assertNull(finding(analyze("a.b.P", twoBad), GuardAnalyzerImpl.NEAR_MISS_IMMUTABLE, "a.b.P"));
    }

    @DisplayName("a genuinely immutable class is silent")
    @Test
    public void testGenuineImmutableSilent() throws IOException {
        @Language("java") String immutable = """
                package a.b;
                public class P {
                    private final int a;
                    private final String b;
                    private final long c;
                    P(int a, String b, long c) { this.a = a; this.b = b; this.c = c; }
                    public int a() { return a; }
                    public String b() { return b; }
                    public long c() { return c; }
                }
                """;
        List<Message> messages = analyze("a.b.P", immutable);
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_IMMUTABLE, "a.b.P"));
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_INDEPENDENT, "a.b.P"));
    }

    // ---- @Immutable subsumes @Independent: a single exposed field yields an immutable, not independent, near-miss ----

    @Language("java")
    private static final String SINGLE_EXPOSED_FIELD = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Q {
                private final int x;
                private final int y;
                private final List<String> data = new ArrayList<>();
                Q(int x, int y) { this.x = x; this.y = y; }
                public int x() { return x; }
                public int y() { return y; }
                public List<String> data() { return data; } // exposes the field: dependent (immutability rule 3)
            }
            """;

    @DisplayName("a single exposed field is an @Immutable near-miss (rule 3), and not also an @Independent one")
    @Test
    public void testImmutableSubsumesIndependent() throws IOException {
        List<Message> messages = analyze("a.b.Q", SINGLE_EXPOSED_FIELD);
        Message immutable = finding(messages, GuardAnalyzerImpl.NEAR_MISS_IMMUTABLE, "a.b.Q");
        assertNotNull(immutable, () -> "expected a @Immutable near-miss on Q, have: "
                                       + messages.stream().filter(x -> x.category().startsWith("near-miss"))
                                               .map(x -> x.category() + ":" + x.info().fullyQualifiedName()).toList());
        assertTrue(immutable.message().contains("1 of its 3 fields"), immutable.message());
        Message cause = immutable.causes().getFirst();
        assertTrue(cause.message().contains("field 'data'") && cause.message().contains("rule 3"), cause.message());
        // subsumption: no separate @Independent near-miss for the same type
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_INDEPENDENT, "a.b.Q"),
                "@Immutable subsumes independence; the independent near-miss must be suppressed");
    }

    // ---- @Independent near-miss: a dependent field where immutability is blocked by more than one field ----

    @Language("java")
    private static final String DEPENDENT_ONLY = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class R {
                private int count;                         // non-final (setCount): immutability rule 0, but independence-neutral
                private final int id;
                private final List<String> data = new ArrayList<>();
                R(int id) { this.id = id; }
                public int id() { return id; }
                public int count() { return count; }
                public void setCount(int c) { this.count = c; }
                public List<String> data() { return data; } // exposes the field: the only independence blocker
            }
            """;

    @DisplayName("when immutability is blocked by two fields but independence by one, an @Independent near-miss fires")
    @Test
    public void testIndependentTypeNearMiss() throws IOException {
        List<Message> messages = analyze("a.b.R", DEPENDENT_ONLY);
        // immutability has two blocking fields (count rule 0, data rule 3) -> suppressed
        assertNull(finding(messages, GuardAnalyzerImpl.NEAR_MISS_IMMUTABLE, "a.b.R"),
                "two blocking fields is above maxBlockingSlots for immutability");
        Message independent = finding(messages, GuardAnalyzerImpl.NEAR_MISS_INDEPENDENT, "a.b.R");
        assertNotNull(independent, () -> "expected an @Independent near-miss on R, have: "
                                         + messages.stream().filter(x -> x.category().startsWith("near-miss"))
                                                 .map(x -> x.category() + ":" + x.info().fullyQualifiedName()).toList());
        assertTrue(independent.message().contains("would satisfy @Independent"), independent.message());
        assertTrue(independent.message().contains("1 of its 3 fields"), independent.message());
        Message cause = independent.causes().getFirst();
        assertTrue(cause.message().contains("field 'data'"), cause.message());
    }
}
