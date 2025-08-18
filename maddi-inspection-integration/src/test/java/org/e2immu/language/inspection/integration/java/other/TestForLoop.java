package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestForLoop extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;

            class X {
               interface I { String get(); }
               static class Y {
                   final I data[];
               }
               void method(Y y) {
                   for(var i: y.data) {
                       System.out.println(i.get());
                   }
               }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);

    }
}
