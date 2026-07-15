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
 * Guard mode, {@code @Independent} on a type (§080): external modifications must not reach the accessible content.
 * The guard fires only on an explicitly written annotation — the contract map cannot be trusted here, because
 * {@code AnnotationToProperty} synthesizes {@code INDEPENDENT_TYPE} for every unannotated type.
 */
public class TestGuardIndependentType extends CommonTest {

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
    private static final String DEPENDENT_FIELD = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import java.util.Set;

            public class X {
                @Independent
                static class HoldsSomeoneElsesSet<T> {
                    private final Set<T> set;

                    public HoldsSomeoneElsesSet(Set<T> set) { this.set = set; } // no copy: the field is dependent

                    public int size() { return set.size(); }
                }
            }
            """;

    @DisplayName("a dependent field under an explicit @Independent type contract is reported and named")
    @Test
    public void testDependentField() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", DEPENDENT_FIELD));
        assertFalse(violations.isEmpty(), "expected a violation for the dependent field");
        Message v = violations.getFirst();
        assertTrue(v.level().isError());
        assertEquals("a.b.X.HoldsSomeoneElsesSet.set", v.info().fullyQualifiedName());
        assertTrue(v.message().contains("is dependent"), v.message());
        assertTrue(v.message().contains("@Independent contract on a.b.X.HoldsSomeoneElsesSet"), v.message());
        assertEquals(1, v.causes().size());
        assertTrue(v.causes().getFirst().message().contains("@Independent contracted on HoldsSomeoneElsesSet"),
                v.causes().getFirst().message());
    }

    @Language("java")
    private static final String CONFORMING = """
            package a.b;
            import org.e2immu.annotation.Container;
            import org.e2immu.annotation.Independent;
            import java.util.HashSet;
            import java.util.Set;

            public class X {
                // §080: a type need not be immutable to be independent; it communicates only via immutable types
                @Independent
                @Container
                static class GetterSetter {
                    private int i;

                    public int getI() { return i; }

                    public void setI(int i) { this.i = i; }
                }

                @Independent
                static class OwnsItsSet<T> {
                    private final Set<T> set;

                    public OwnsItsSet(Set<T> input) { this.set = new HashSet<>(input); } // copy: independent

                    public int size() { return set.size(); }
                }
            }
            """;

    @DisplayName("positive control: genuinely independent types raise nothing")
    @Test
    public void testConforming() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", CONFORMING));
        assertTrue(violations.isEmpty(), "expected no violations, have: "
                                         + violations.stream().map(Message::message).toList());
    }

    @Language("java")
    private static final String NO_ANNOTATION = """
            package a.b;
            import java.util.Set;

            public class X {
                // no annotation at all: AnnotationToProperty still synthesizes INDEPENDENT_TYPE for this type.
                // The guard must not treat that as a contract.
                static class Unannotated<T> {
                    private final Set<T> set;

                    public Unannotated(Set<T> set) { this.set = set; }

                    public int size() { return set.size(); }
                }

                // all non-private methods speak only primitives: simpleComputeIndependent contracts this as
                // INDEPENDENT, again without the user writing anything.
                static class OnlyPrimitives {
                    private final StringBuilder sb = new StringBuilder();

                    public void add(String s) { sb.append(s); }

                    public int length() { return sb.length(); }
                }
            }
            """;

    /**
     * Regression test for the {@code hasExplicitIndependentAnnotation} gate. Note what this does and does not prove.
     * The gate's premise is real and was verified by instrumenting the branch: with the gate removed, both
     * {@code a.b.X} and {@code a.b.X.OnlyPrimitives} — neither of which carries any annotation — enter
     * {@code guardIndependentType} with a synthesized {@code contract=@Independent}. What could not be constructed is
     * an unannotated type that also produces a false *finding*: the very condition that synthesizes INDEPENDENT (a
     * non-private API speaking only primitives) tends to leave every field independent too. So this test passes with
     * and without the gate today; the gate is there on principle — never key a contract off a synthesized value —
     * and guards against that coincidence being broken by a future change to {@code simpleComputeIndependent} or to
     * what makes a field dependent.
     */
    @DisplayName("no annotation, no contract: unannotated types are not considered for the independence guard")
    @Test
    public void testUnannotatedIsNotGuarded() throws IOException {
        List<Message> violations = violations(analyzeWithGuard("a.b.X", NO_ANNOTATION));
        assertTrue(violations.isEmpty(), "unannotated types must never be guarded, have: "
                                         + violations.stream().map(Message::message).toList());
    }
}
