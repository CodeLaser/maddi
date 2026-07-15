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

    @DisplayName("a dependent implementation of an @Independent abstract method is reported; the copy is not")
    @Test
    public void testViolation() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.X", INDEPENDENT_CONTRACT);
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(1, violations.size(), "expected exactly one violation (BadSource only), have: "
                                           + violations.stream().map(Message::message).toList());
        Message violation = violations.getFirst();
        assertTrue(violation.level().isError());
        assertEquals("a.b.X.BadSource.items()", violation.info().fullyQualifiedName());
        assertTrue(violation.message().contains("dependent"), violation.message());
        assertTrue(violation.message().contains("@Independent contract on a.b.X.Source.items()"),
                violation.message());
        assertNotNull(violation.source());

        // the why-chain: [ blame, contract location ] — the blame names the exposed field and is located at the
        // return statement that exposes it
        assertEquals(2, violation.causes().size(), violation.causes().stream().map(Message::message).toList()
                .toString());
        Message blame = violation.causes().getFirst();
        assertTrue(blame.message().contains("returns the field 'list' itself, exposing it"), blame.message());
        assertNotNull(blame.source(), "the blame must be located at the offending return statement");

        Message contractLocation = violation.causes().get(1);
        assertTrue(contractLocation.message().contains("@Independent contracted here"), contractLocation.message());
        assertEquals("a.b.X.Source.items()", contractLocation.info().fullyQualifiedName());

        // the conforming copy-returning implementation must stay silent
        assertTrue(messages.stream().noneMatch(m -> m.message().contains("GoodSource")),
                messages.stream().map(Message::message).toList().toString());
    }
}
