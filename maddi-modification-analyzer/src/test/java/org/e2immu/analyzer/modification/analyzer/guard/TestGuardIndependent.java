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
 * Guard mode, second contract kind: {@code @Independent} on an abstract method — every implementation must not be
 * dependent (must not expose or share its mutable internals). Mirrors the {@code @NotModified}/@Container checks:
 * only implementations, whose independence is genuinely computed (not trusted from the contract), are inspected.
 */
public class TestGuardIndependent extends CommonTest {

    @Language("java")
    private static final String INDEPENDENT_CONTRACT = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import java.util.ArrayList;
            import java.util.List;

            public class X {

                interface Source {
                    @Independent
                    List<String> items();
                }

                static class BadSource implements Source {
                    private final List<String> list = new ArrayList<>();

                    @Override
                    public List<String> items() {
                        return list; // exposes the internal mutable list: dependent, violates @Independent
                    }
                }

                static class GoodSource implements Source {
                    private final List<String> list = new ArrayList<>();

                    @Override
                    public List<String> items() {
                        return List.copyOf(list); // returns an immutable copy: independent
                    }
                }

                // the field still escapes, but never as a bare 'return list': no syntactic match to find
                static class IndirectSource implements Source {
                    private final List<String> list = new ArrayList<>();

                    private List<String> wrap(List<String> in) { return in; }

                    @Override
                    public List<String> items() {
                        return wrap(list);
                    }
                }
            }
            """;

    private List<Message> analyzeWithGuard(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true).build());
        analyzer.analyze(analysisOrder);
        return analyzer.messages();
    }

    @DisplayName("dependent implementations of an @Independent abstract method are reported; the copy is not")
    @Test
    public void testViolation() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.X", INDEPENDENT_CONTRACT);
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(2, violations.size(), "expected BadSource and IndirectSource, have: "
                                           + violations.stream().map(Message::message).toList());

        Message violation = byInfo(violations, "a.b.X.BadSource.items()");
        assertTrue(violation.level().isError());
        assertTrue(violation.message().contains("dependent"), violation.message());
        assertTrue(violation.message().contains("@Independent contract on a.b.X.Source.items()"),
                violation.message());
        assertNotNull(violation.source());

        // the why-chain: [ blame, contract location ]. The blame comes from the method's computed METHOD_LINKS —
        // the same links doIndependentMethod folded into the DEPENDENT verdict — and names the exposed field.
        assertEquals(2, violation.causes().size(), violation.causes().stream().map(Message::message).toList()
                .toString());
        Message blame = violation.causes().getFirst();
        assertTrue(blame.message().contains("its return value is linked to the field 'list', which is mutable"),
                blame.message());
        assertNotNull(blame.source());

        Message contractLocation = violation.causes().get(1);
        assertTrue(contractLocation.message().contains("@Independent contracted here"), contractLocation.message());
        assertEquals("a.b.X.Source.items()", contractLocation.info().fullyQualifiedName());

        // 'return wrap(list)' has no bare 'return <field>' to find: a CST scan would report this violation with no
        // "where" at all. The links follow the object, so the field is still named.
        Message indirect = byInfo(violations, "a.b.X.IndirectSource.items()");
        assertEquals(2, indirect.causes().size(), indirect.causes().stream().map(Message::message).toList()
                .toString());
        assertTrue(indirect.causes().getFirst().message()
                        .contains("its return value is linked to the field 'list', which is mutable"),
                indirect.causes().getFirst().message());

        // the conforming copy-returning implementation must stay silent
        assertTrue(messages.stream().noneMatch(m -> m.message().contains("GoodSource")),
                messages.stream().map(Message::message).toList().toString());
    }

    private Message byInfo(List<Message> violations, String fqn) {
        return violations.stream().filter(m -> fqn.equals(m.info().fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no violation on " + fqn + ", have: "
                                                      + violations.stream().map(m -> m.info().fullyQualifiedName())
                                                              .toList()));
    }
}
