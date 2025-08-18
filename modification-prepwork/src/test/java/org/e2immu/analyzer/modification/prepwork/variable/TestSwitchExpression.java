package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.SwitchExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSwitchExpression extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            import java.util.ArrayList;import java.util.List;
            record X(List<String> list, int k) {
                int method() {
                    return switch(k) {
                        case 0 -> list.get(0).length();
                        case 1 -> {
                            System.out.println("?");
                            yield k+3;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        assertTrue(X.typeNature().isRecord());

        MethodInfo method = X.findUniqueMethod("method", 0);
        SwitchExpression switchExpression = (SwitchExpression) method.methodBody().statements().getFirst().expression();
        Statement s0 = switchExpression.entries().getFirst().statement();
        assertEquals("5-23:5-42", s0.source().compact2());
        VariableData vd0 = VariableDataImpl.of(s0);
        assertEquals("X.list, X.this, java.util.List._synthetic_list#X.list, java.util.List._synthetic_list#X.list[0]",
                vd0.knownVariableNamesToString());
        Block b1 = (Block) switchExpression.entries().get(1).statement();
        Statement s1 = b1.statements().get(1);
        assertEquals("8-17:8-26", s1.source().compact2());
        assertNotNull(VariableDataImpl.of(s1).knownVariableNamesToString());
    }
}
