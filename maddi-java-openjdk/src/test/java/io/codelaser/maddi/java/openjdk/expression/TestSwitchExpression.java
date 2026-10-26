package io.codelaser.maddi.java.openjdk.expression;

import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.statement.SwitchEntry;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.YieldStatement;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSwitchExpression extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            import java.util.List;
            record X(List<String> list, int k) {
                int method() {
                    return switch(k) {
                        case 0 -> list.getFirst().length();
                        case 1 -> {
                            System.out.println("?");
                            yield k+3;
                        }
                        case 2 -> {
                            int v = 5;
                            System.out.println(v);
                            yield v;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("X", INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 0);
        Statement returnStatement = method.methodBody().statements().getFirst();
        SwitchExpression switchExpression = (SwitchExpression) returnStatement.expression();
        Statement s0 = switchExpression.entries().getFirst().statement();
        assertEquals("0.0.0", s0.source().index());
        Statement s11 = switchExpression.entries().get(1).statementAsBlock().statements().get(1);
        assertEquals("0.1.1", s11.source().index());
        assertInstanceOf(YieldStatement.class, s11);
    }

    // JEP 456: a case label may carry several patterns as long as none binds a variable, e.g. 'case A _, B _ ->'.
    // Mirrors com.sun.tools.jdeps.Dependencies ('case BaseTypeSig _, TypeVarSig _ ->'), which crashed the scan.
    @Language("java")
    public static final String INPUT2 = """
            class X {
                sealed interface S permits A, B, C {}
                record A() implements S {}
                record B() implements S {}
                record C() implements S {}
                int method(S s) {
                    return switch (s) {
                        case A _, B _ -> 1;
                        case C _ -> 2;
                    };
                }
            }
            """;

    @DisplayName("multiple binding-free type patterns in one case label")
    @Test
    public void test2() {
        TypeInfo X = scan("X", INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement returnStatement = method.methodBody().statements().getFirst();
        SwitchExpression switchExpression = (SwitchExpression) returnStatement.expression();
        assertEquals(2, switchExpression.entries().size());
    }

    // The patterns after the first are part of the entry: their types are referenced (import maintenance counts on it)
    // and they are printed. trino 817f7034667, PathEvaluationVisitor: 'case VarcharType _, CharType _ ->' reported
    // VarcharType only, and a dead-code removal dropped 'import io.trino.spi.type.CharType;' from under two live uses.
    @DisplayName("every pattern of a multi-pattern case label is referenced and printed")
    @Test
    public void test3() {
        TypeInfo X = scan("X", INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement returnStatement = method.methodBody().statements().getFirst();
        SwitchExpression switchExpression = (SwitchExpression) returnStatement.expression();
        SwitchEntry entry = switchExpression.entries().getFirst();
        List<String> referenced = entry.typesReferenced(null)
                .map(r -> r.typeInfo().simpleName()).distinct().sorted().toList();
        assertEquals(List.of("A", "B"), referenced,
                "types the entry 'case A _, B _ -> 1' reports as referenced (left: both patterns; right: reported)");
        // whitespace collapsed: OutputBuilder.toString() separates the tokens of a pattern with two spaces
        String printed = entry.print(QualificationImpl.SIMPLE_ONLY).toString().replaceAll("\\s+", " ");
        assertTrue(printed.contains("A _") && printed.contains("B _"),
                "the entry 'case A _, B _ -> 1' prints both patterns; it printed: " + printed);
    }

    // A walk over the whole switch reaches every pattern of every case, not only the first of each.
    @DisplayName("walking a switch expression reaches every pattern of a multi-pattern case label")
    @Test
    public void test4() {
        TypeInfo X = scan("X", INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        SwitchExpression switchExpression = (SwitchExpression) method.methodBody().statements().getFirst().expression();
        List<String> visited = new ArrayList<>();
        switchExpression.visit(e -> {
            if (e instanceof RecordPattern rp && rp.localVariable() != null) {
                visited.add(rp.localVariable().parameterizedType().typeInfo().simpleName());
            }
            return true;
        });
        assertEquals(List.of("A", "B", "C"), visited,
                "patterns reached walking 'case A _, B _ -> 1; case C _ -> 2' (left: all three; right: reached)");
    }
}
