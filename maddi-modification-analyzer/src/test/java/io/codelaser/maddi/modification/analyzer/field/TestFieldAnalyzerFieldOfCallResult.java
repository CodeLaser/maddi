package io.codelaser.maddi.modification.analyzer.field;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFieldAnalyzerFieldOfCallResult extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.cr;
            import java.util.Objects;
            class L {
                static class Node {
                    Node next;
                }
                private Node tail = new Node();
                private Node last = new Node();
                void append(Node node) {
                    Objects.requireNonNull(tail).next = node;
                }
                void appendLast(Node node) {
                    self().next = node;
                }
                private Node self() {
                    return last;
                }
            }
            """;

    @DisplayName("assigning a field of a call's result modifies the object the result stands for")
    @Test
    public void test() {
        TypeInfo L = javaInspector.parse("a.cr.L", INPUT);
        List<Info> ao = prepWork(L);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(true).build());
        analyzer.analyze(ao);
        // guava's LinkedListMultimap.addNode: 'requireNonNull(tail).next = node'. The assignment marked the scope
        // chain of its target modified, but a scope that is an expression (a call) has no scope VARIABLE, so
        // nothing was marked: 'tail' read as unmodified. A modifying call on the same result ('.setNext(node)')
        // was fine.
        Value.Bool tailUnmodified = L.getFieldByName("tail", true).analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
        Value.Bool lastUnmodified = L.getFieldByName("last", true).analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
        assertTrue(tailUnmodified != null && tailUnmodified.isFalse()
                   && lastUnmodified != null && lastUnmodified.isFalse(),
                "tail unmodified=" + tailUnmodified + ", last unmodified=" + lastUnmodified);
    }
}
