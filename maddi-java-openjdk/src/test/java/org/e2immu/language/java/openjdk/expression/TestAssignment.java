package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
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

    // a parenthesised increment/decrement target, e.g. '(this.refCount)++' as in sun.security.pkcs11.P11Key;
    // the target must be unwrapped to the underlying variable rather than cast straight to VariableExpression
    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class C {
                int refCount;
                int method() {
                    int a = (this.refCount)++;
                    int b = --(this.refCount);
                    return a + b;
                }
            }
            """;

    @DisplayName("parenthesised increment/decrement target")
    @Test
    public void test2() {
        TypeInfo C = scan("a.b.C", INPUT2);
        MethodInfo method = C.findUniqueMethod("method", 0);
        assertEquals(3, method.methodBody().statements().size());
        assertEquals("int a=this.refCount++;", method.methodBody().statements().get(0).toString());
        assertEquals("int b=--this.refCount;", method.methodBody().statements().get(1).toString());
    }
}
