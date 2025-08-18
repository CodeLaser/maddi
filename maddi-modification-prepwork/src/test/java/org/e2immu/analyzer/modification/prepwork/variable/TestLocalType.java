package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLocalType extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                interface A { void method(String s); }
                A make(String t) {
                    final class C implements A {
                        @Override
                        void method(String s) {
                            System.out.println(s+t);
                        }
                    }
                    return new C();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("make", 1);
        LocalTypeDeclaration ltd = (LocalTypeDeclaration) method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(ltd);
        assertEquals("a.b.X.make(String):0:t, java.lang.System.out", vd0.knownVariableNamesToString());
    }
}
