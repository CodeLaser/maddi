package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOverrides extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.function.Function;
            class X {
                ArrayList<Integer> intList;
            
                Integer getInt(int index) {
                    return intList.get(index);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);
        MethodInfo getInt = X.findUniqueMethod("getInt", 1);
        MethodCall mc = (MethodCall) getInt.methodBody().statements().getFirst().expression();
        assertEquals("java.util.AbstractList.get(int),java.util.List.get(int)",
                mc.methodInfo().overrides().stream().map(Object::toString).sorted().collect(Collectors.joining(",")));

    }
}
