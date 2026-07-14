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
 * Guard mode, first scenario from the design doc: the user contracts an interface to be @Container;
 * the analyzer verifies all implementations and reports violations with an explanation of why.
 */
public class TestGuardContainer extends CommonTest {

    @Language("java")
    private static final String CONTAINER_CONTRACT = """
            package a.b;
            import org.e2immu.annotation.Container;
            import java.util.ArrayList;
            import java.util.List;

            public class X {

                @Container
                interface ErrorRegistry {
                    void add(ErrorMessage message);

                    int size();
                }

                static class ErrorMessage {
                    private String msg;

                    public void setMsg(String s) {
                        this.msg = s;
                    }

                    public String getMsg() {
                        return msg;
                    }
                }

                static class BadRegistry implements ErrorRegistry {
                    private final List<ErrorMessage> list = new ArrayList<>();

                    @Override
                    public void add(ErrorMessage message) {
                        message.setMsg("seen"); // modifies the argument: violates @Container on ErrorRegistry
                        list.add(message);
                    }

                    @Override
                    public int size() {
                        return list.size();
                    }
                }

                static class GoodRegistry implements ErrorRegistry {
                    private final List<ErrorMessage> list = new ArrayList<>();

                    @Override
                    public void add(ErrorMessage message) {
                        list.add(message);
                    }

                    @Override
                    public int size() {
                        return list.size();
                    }
                }
            }
            """;

    private List<Message> analyzeWithGuard(String fqn, String source, boolean guard) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(guard).build());
        analyzer.analyze(analysisOrder);
        return analyzer.messages();
    }

    @DisplayName("a modifying implementation of a @Container interface is reported, with the why-chain")
    @Test
    public void testViolation() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.X", CONTAINER_CONTRACT, true);
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(1, violations.size(), "expected exactly one contract violation, have: "
                                           + violations.stream().map(Message::message).toList());
        Message violation = violations.getFirst();
        assertTrue(violation.level().isError());

        // the finding blames the offending parameter of the implementation...
        assertEquals("a.b.X.BadRegistry.add(a.b.X.ErrorMessage):0:message", violation.info().fullyQualifiedName());
        assertTrue(violation.message().contains("parameter 'message'"), violation.message());
        assertTrue(violation.message().contains("@Container contract on a.b.X.ErrorRegistry"), violation.message());
        assertNotNull(violation.source(), "the finding must be locatable (line/col)");

        // the why-chain now has two branches: (1) the blame — the statement that modifies the parameter...
        assertEquals(2, violation.causes().size());
        Message blame = violation.causes().getFirst();
        assertTrue(blame.message().contains("message.setMsg(...)") && blame.message().contains("modifies 'message'"),
                blame.message());
        assertNotNull(blame.source(), "the blame must point at the modifying statement (line/col)");

        // ... and (2) the contract provenance: implements-link, contract location beneath it
        Message implementsLink = violation.causes().get(1);
        assertTrue(implementsLink.message().contains("implements a.b.X.ErrorRegistry.add"), implementsLink.message());
        assertEquals(1, implementsLink.causes().size());
        Message contractLocation = implementsLink.causes().getFirst();
        assertTrue(contractLocation.message().contains("@Container contracted on ErrorRegistry"),
                contractLocation.message());
        assertEquals("a.b.X.ErrorRegistry", contractLocation.info().fullyQualifiedName());
        assertNotNull(contractLocation.source());
    }

    @DisplayName("a conforming implementation stays silent (positive control)")
    @Test
    public void testConformingImplementation() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.X", CONTAINER_CONTRACT, true);
        assertTrue(messages.stream().noneMatch(m -> m.message().contains("GoodRegistry")),
                "GoodRegistry conforms and must not be reported: "
                + messages.stream().map(Message::message).toList());
    }

    @DisplayName("guard disabled: same code, no contract-violation findings")
    @Test
    public void testGuardDisabled() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.X", CONTAINER_CONTRACT, false);
        assertTrue(messages.stream().noneMatch(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())),
                messages.stream().map(Message::message).toList().toString());
    }

    @Language("java")
    private static final String CONCRETE_CONTAINER = """
            package a.b;
            import org.e2immu.annotation.Container;

            @Container
            public class Y {
                private int count;

                public void take(StringBuilder sb) {
                    sb.append('!'); // modifies the argument: violates the type's own @Container contract
                    count++;
                }
            }
            """;

    @DisplayName("a concrete @Container class modifying a parameter of its own method is reported directly")
    @Test
    public void testConcreteClass() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.Y", CONCRETE_CONTAINER, true);
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(1, violations.size(), messages.stream().map(Message::message).toList().toString());
        Message violation = violations.getFirst();
        assertTrue(violation.message().contains("parameter 'sb'"), violation.message());
        // no implements-link needed (the violating method is in the contracted type itself), but the blame is:
        // causes = [ blame: sb.append(...) modifies sb, contract location ]
        assertEquals(2, violation.causes().size());
        Message blame = violation.causes().getFirst();
        assertTrue(blame.message().contains("sb.append(...)") && blame.message().contains("modifies 'sb'"),
                blame.message());
        assertNotNull(blame.source());
        assertTrue(violation.causes().get(1).message().contains("@Container contracted on Y"),
                violation.causes().get(1).message());
    }

    @Language("java")
    private static final String NOT_MODIFIED_CONTRACT = """
            package a.b;
            import org.e2immu.annotation.NotModified;

            public class Z {

                interface HasSize {
                    @NotModified
                    int size();
                }

                static class Cheater implements HasSize {
                    private int count;

                    @Override
                    public int size() {
                        count++; // modifying: violates the @NotModified contract on HasSize.size()
                        return count;
                    }
                }
            }
            """;

    @DisplayName("a modifying implementation of a @NotModified abstract method is reported")
    @Test
    public void testNotModifiedMethod() throws IOException {
        List<Message> messages = analyzeWithGuard("a.b.Z", NOT_MODIFIED_CONTRACT, true);
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(1, violations.size(), messages.stream().map(Message::message).toList().toString());
        Message violation = violations.getFirst();
        assertTrue(violation.message().contains("a.b.Z.Cheater.size()"), violation.message());
        assertTrue(violation.message().contains("@NotModified contract on a.b.Z.HasSize.size()"),
                violation.message());
        // causes = [ blame: assigns field 'count', contract location ]
        assertEquals(2, violation.causes().size());
        Message blame = violation.causes().getFirst();
        assertTrue(blame.message().contains("assigns field 'count'"), blame.message());
        assertNotNull(blame.source());
        assertTrue(violation.causes().get(1).message().contains("@NotModified contracted here"),
                violation.causes().get(1).message());
    }
}
