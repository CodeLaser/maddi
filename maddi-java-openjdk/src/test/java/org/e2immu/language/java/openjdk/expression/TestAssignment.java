package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAssignment extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class C {
                void method1(int k) {
                    int i = 3;
                    i += k;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = scan("a.b.C", INPUT1);
        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        assertEquals("i+=k;", method1.methodBody().statements().get(1).toString());
    }

}
