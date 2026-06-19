package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCast extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.io.Serializable;class C {
              void method(Object someValue, Serializable s, Runnable r) {
                  Object o = (Serializable & Runnable) someValue;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 3);
        LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        assertEquals("java.io.Serializable&Runnable",
                lvc.localVariable().assignmentExpression().parameterizedType().toString());
    }
}
