package org.e2immu.analyzer.modification.analyzer.method;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLambda extends CommonTest {
    @Language("java")
    String INPUT1 = """
            package a.b.ii;
            import java.util.List;
            import java.util.stream.Stream;
            public class C1 {
                interface II {
                    void method1(String s);
                    void method2(int i);
                }
                void method1(List<II> in, String s) {
                    boolean b = in.stream().anyMatch(ii -> { ii.method1(s); return true; });
                }
                void method2(List<II> in, int n) {
                    boolean b = in.stream().anyMatch(ii -> { ii.method2(n); return true; });
                }
            }
            """;

    @DisplayName("using @Identity method")
    @Test
    public void test1() {
        TypeInfo C1 = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(C1);
        analyzer.go(ao);

        MethodInfo method2 = C1.findUniqueMethod("method2", 2);
        LocalVariableCreation lvc = (LocalVariableCreation) method2.methodBody().statements().getFirst();
        ParameterInfo in = method2.parameters().getFirst();

        VariableData vd0 = VariableDataImpl.of(lvc);
        VariableInfo vi0In = vd0.variableInfo(in);
        assertEquals("0:in.§$s∋0:ii", vi0In.linkedVariables().toString());

        MethodCall anyMatch = (MethodCall) lvc.localVariable().assignmentExpression();
        Lambda lambda = (Lambda) anyMatch.parameterExpressions().getFirst();
        Statement s0 = lambda.methodBody().statements().getFirst();
        var aMap = anyMatch.analysis().getOrNull(VARIABLES_LINKED_TO_OBJECT, ValueImpl.VariableBooleanMapImpl.class);
        assertEquals("""
                a.b.ii.C1.$1.test(a.b.ii.C1.II):0:ii=false, a.b.ii.C1.method2(java.util.List<a.b.ii.C1.II>,int):0:in=true\
                """, aMap.toString());

        VariableData vdL0 = VariableDataImpl.of(s0);
        VariableInfo vi0ii = vdL0.variableInfo(lambda.methodInfo().parameters().getFirst());
        //inside the lambda, we cannot know of the link to "in"
        assertEquals("-", vi0ii.linkedVariables().toString());
        MethodCall mc2 = (MethodCall) s0.expression();
        var map = mc2.analysis().getOrNull(VARIABLES_LINKED_TO_OBJECT, ValueImpl.VariableBooleanMapImpl.class);
        //hence, the VL2O also cannot see "in"
        assertEquals("a.b.ii.C1.$1.test(a.b.ii.C1.II):0:ii=true", map.toString());
    }
}
