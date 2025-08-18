package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseMethodTypeParameter extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              interface I {}
              <T> T method(String s) { return null; }
              <T, S extends I> T method2(S s) { return null; }
              private <T> C(T t, int i) { }
              <T> C(T t) { }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        assertEquals("Type param T", methodInfo.returnType().toString());
        assertSame(methodInfo, methodInfo.returnType().typeParameter().getOwner().getRight());

        MethodInfo methodInfo2 = typeInfo.findUniqueMethod("method2", 1);
        assertEquals("Type param T", methodInfo.returnType().toString());
        assertSame(methodInfo, methodInfo.returnType().typeParameter().getOwner().getRight());
        assertEquals(2, methodInfo2.typeParameters().size());
        TypeParameter tp1 = methodInfo2.typeParameters().get(1);
        assertEquals("S=TP#1 in C.method2", tp1.toString());
        assertSame(methodInfo2, tp1.getOwner().getRight());
        assertTrue(tp1.isMethodTypeParameter());
    }
}
