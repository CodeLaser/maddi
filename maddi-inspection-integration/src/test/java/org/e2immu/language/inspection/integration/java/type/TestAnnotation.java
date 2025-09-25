package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAnnotation extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class C {
                @interface X {
                    int A = 3;
                }
                void method() {
                    System.out.println(X.A);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        TypeInfo X = C.findSubType("X");
        FieldInfo A = X.getFieldByName("A", true);
        assertEquals("3", A.initializer().toString());
    }
}
