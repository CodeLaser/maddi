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
 * Guard mode, hierarchy monotonicity: a subclass override may not weaken a contract carried by a concrete
 * overridden method (the case the abstract→implementations path does not cover).
 */
public class TestGuardHierarchy extends CommonTest {

    @Language("java")
    private static final String NOT_MODIFIED_OVERRIDE = """
            package a.b;
            import org.e2immu.annotation.NotModified;

            public class X {
                static class Base {
                    @NotModified
                    public int compute() { return 1; }
                }

                static class Derived extends Base {
                    private int n;
                    @Override
                    public int compute() { n++; return n; } // modifying override: violates Base's @NotModified
                }

                static class Faithful extends Base {
                    @Override
                    public int compute() { return 2; } // still non-modifying: fine
                }
            }
            """;

    private List<Message> analyze(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true).build());
        analyzer.analyze(analysisOrder);
        return analyzer.messages();
    }

    @DisplayName("a modifying override of a @NotModified concrete method is reported; the faithful override is not")
    @Test
    public void testModifyingOverride() throws IOException {
        List<Message> messages = analyze("a.b.X", NOT_MODIFIED_OVERRIDE);
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(1, violations.size(), "expected exactly one violation (Derived only), have: "
                                           + violations.stream().map(Message::message).toList());
        Message violation = violations.getFirst();
        assertEquals("a.b.X.Derived.compute()", violation.info().fullyQualifiedName());
        assertTrue(violation.message().contains("overrides a.b.X.Base.compute()"), violation.message());
        assertTrue(violation.message().contains("@NotModified"), violation.message());
        // why-chain: [ blame (assigns field 'n'), contract location ]
        assertEquals(2, violation.causes().size());
        assertTrue(violation.causes().getFirst().message().contains("assigns field 'n'"),
                violation.causes().getFirst().message());
        assertTrue(messages.stream().noneMatch(m -> m.message().contains("Faithful")),
                messages.stream().map(Message::message).toList().toString());
    }
}
