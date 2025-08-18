package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypesReferenced extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.lang.annotation.Annotation;
            class X {
                interface Y<A extends Annotation> {
                   A supply();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertEquals("""
                [TypeReference[typeInfo=java.lang.annotation.Annotation, explicit=true], \
                TypeReference[typeInfo=java.lang.annotation.Annotation, explicit=true]]\
                """, typeInfo.typesReferenced().toList().toString());
        // FIXME PRINT space in front of {A
        String expect = """
                package a.b;
                import java.lang.annotation.Annotation;
                class X { interface Y<A extends Annotation> {A supply(); } }
                """;
        assertEquals(expect, javaInspector.print2(typeInfo));
    }
}
