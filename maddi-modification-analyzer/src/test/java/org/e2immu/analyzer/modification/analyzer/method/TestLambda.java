package org.e2immu.analyzer.modification.analyzer.method;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        ParameterInfo in = method2.parameters().getFirst();
        VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
        VariableInfo vi0In = vd0.variableInfo(in);
        assertEquals("-", vi0In.linkedVariables().toString());
        // FIXME how can we link ii.method2(n) / any use of ii to 'in'?
        
    }
}
