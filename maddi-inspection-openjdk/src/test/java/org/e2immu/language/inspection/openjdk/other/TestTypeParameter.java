package org.e2immu.language.inspection.openjdk.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeParameter extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class C {
                interface AnnotationExpression {
                    <T> T extract(String s, T t);
                }
            }
            """;
    @Test
    public void test1() {
        TypeInfo typeInfo = scan(Map.of("a.b.C", INPUT1), List.of()).getFirst();
        TypeInfo annotationExpression = typeInfo.findSubType("AnnotationExpression");
        MethodInfo extract = annotationExpression.methods().getFirst();
        assertEquals("a.b.C.AnnotationExpression.extract(String,T)", extract.fullyQualifiedName());
        assertEquals(1, extract.typeParameters().size());
        assertEquals("T=TP#0 in AnnotationExpression.extract []", extract.typeParameters().getFirst().toStringWithTypeBounds());
    }
}
