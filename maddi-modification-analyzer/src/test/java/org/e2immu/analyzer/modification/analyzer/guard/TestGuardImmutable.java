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
 * Guard mode, {@code @Immutable} on a type: the rules of immutability (§050) are verified one by one, so that a
 * violation names the rule and the member that breaks it. Because a contracted type carries the trusted contract in
 * its own {@code IMMUTABLE_TYPE}, the guard reads the per-field computed values instead.
 */
public class TestGuardImmutable extends CommonTest {

    private List<Message> analyzeWithGuard(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true).build());
        analyzer.analyze(analysisOrder);
        return analyzer.messages();
    }

    private List<Message> violations(List<Message> messages) {
        return messages.stream().filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
    }

    @Language("java")
    private static final String RULE_0 = """
            package a.b;
            import org.e2immu.annotation.Immutable;

            public class X {
                @Immutable
                static class NotFinal {
                    private int count;

                    public void bump() { count++; }

                    public int count() { return count; }
                }
            }
            """;

    @DisplayName("rule 0: an assignable field is reported, naming the field and the rule")
    @Test
    public void testRule0() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", RULE_0));
        assertFalse(violations.isEmpty(), "expected a violation for the assignable field");
        Message v = violations.getFirst();
        assertTrue(v.level().isError());
        assertEquals("a.b.X.NotFinal.count", v.info().fullyQualifiedName());
        assertTrue(v.message().contains("rule 0"), v.message());
        assertTrue(v.message().contains("assignable after construction"), v.message());
        assertNotNull(v.source(), "the finding must be located at the field");
        // the why-chain leads back to the contract
        assertEquals(1, v.causes().size());
        assertTrue(v.causes().getFirst().message().contains("@Immutable contracted on NotFinal"),
                v.causes().getFirst().message());
    }

    @Language("java")
    private static final String RULE_1 = """
            package a.b;
            import org.e2immu.annotation.Immutable;
            import java.util.ArrayList;
            import java.util.List;

            public class X {
                @Immutable
                static class ModifiesField {
                    private final List<String> list = new ArrayList<>();

                    public void add(String s) { list.add(s); }

                    public int size() { return list.size(); }
                }
            }
            """;

    @DisplayName("rule 1: a modified field is reported")
    @Test
    public void testRule1() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", RULE_1));
        assertFalse(violations.isEmpty(), "expected a violation for the modified field");
        Message v = violations.getFirst();
        assertEquals("a.b.X.ModifiesField.list", v.info().fullyQualifiedName());
        assertTrue(v.message().contains("rule 1"), v.message());
        assertTrue(v.message().contains("is modified"), v.message());
    }

    @Language("java")
    private static final String RULE_2 = """
            package a.b;
            import org.e2immu.annotation.Immutable;
            import java.util.ArrayList;
            import java.util.List;

            public class X {
                @Immutable
                static class PublicMutableField {
                    public final List<String> list = new ArrayList<>();

                    public int size() { return list.size(); }
                }
            }
            """;

    @DisplayName("rule 2: a non-private field of mutable type is reported")
    @Test
    public void testRule2() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", RULE_2));
        assertFalse(violations.isEmpty(), "expected a violation for the public mutable field");
        Message v = violations.getFirst();
        assertEquals("a.b.X.PublicMutableField.list", v.info().fullyQualifiedName());
        assertTrue(v.message().contains("rule 2"), v.message());
        assertTrue(v.message().contains("not private"), v.message());
    }

    @Language("java")
    private static final String RULE_3 = """
            package a.b;
            import org.e2immu.annotation.Immutable;
            import java.util.HashSet;
            import java.util.Set;

            public class X {
                @Immutable(hc = true)
                static class ExposesViaGetter<T> {
                    private final Set<T> data;

                    public ExposesViaGetter(Set<T> ts) { this.data = new HashSet<>(ts); }

                    public Set<T> getSet() { return data; } // exposure: the field escapes
                }
            }
            """;

    @DisplayName("rule 3: a field exposed through a getter is reported, blaming the getter")
    @Test
    public void testRule3() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", RULE_3));
        assertFalse(violations.isEmpty(), "expected a violation for the exposed field");
        Message v = violations.getFirst();
        assertEquals("a.b.X.ExposesViaGetter.data", v.info().fullyQualifiedName());
        assertTrue(v.message().contains("rule 3"), v.message());
        assertTrue(v.message().contains("dependent"), v.message());

        // the constructor copies, so the only link out is the getter's return value
        assertEquals(2, v.causes().size(), v.causes().stream().map(Message::message).toList().toString());
        Message blame = v.causes().getFirst();
        assertTrue(blame.message().contains("linked to the return value of method 'getSet'"), blame.message());
        assertNotNull(blame.source());
    }

    @Language("java")
    private static final String CONFORMING = """
            package a.b;
            import org.e2immu.annotation.Immutable;
            import java.util.List;
            import java.util.Set;

            public class X {
                @Immutable(hc = true)
                static class Good<T> {
                    private final List<T> list;

                    public Good(List<T> input) { this.list = List.copyOf(input); }

                    public int size() { return list.size(); }
                }

                @Immutable
                static class DeeplyGood {
                    public final int x;
                    public final String message;

                    public DeeplyGood(int x, String message) { this.x = x; this.message = message; }
                }
            }
            """;

    @DisplayName("positive control: genuinely immutable types raise nothing")
    @Test
    public void testConforming() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", CONFORMING));
        assertTrue(violations.isEmpty(), "expected no violations, have: "
                                         + violations.stream().map(Message::message).toList());
    }

    @Language("java")
    private static final String EVENTUAL = """
            package a.b;
            import org.e2immu.annotation.ImmutableContainer;
            import org.e2immu.annotation.eventual.Mark;
            import org.e2immu.annotation.eventual.Only;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.stream.Stream;

            public class X {
                @ImmutableContainer(after = "frozen")
                static class SimpleImmutableSet<T> {
                    private final Set<T> set = new HashSet<>();
                    private boolean frozen;

                    @Only(before = "frozen")
                    public void add(T t) {
                        if (frozen) throw new IllegalStateException();
                        set.add(t);
                    }

                    @Mark("frozen")
                    public void freeze() {
                        if (frozen) throw new IllegalStateException();
                        frozen = true;
                    }

                    @Only(after = "frozen")
                    public Stream<T> stream() {
                        if (!frozen) throw new IllegalStateException();
                        return set.stream();
                    }
                }
            }
            """;

    @DisplayName("eventual immutability (after=...) is not guarded: its fields are assignable by design")
    @Test
    public void testEventualIsSkipped() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", EVENTUAL));
        assertTrue(violations.isEmpty(), "eventually immutable types must not be guarded, have: "
                                         + violations.stream().map(Message::message).toList());
    }

    @DisplayName("guard disabled: no findings")
    @Test
    public void testGuardDisabled() throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse("a.b.X", RULE_0);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(false).build());
        analyzer.analyze(analysisOrder);
        assertTrue(violations(analyzer.messages()).isEmpty());
    }
}
