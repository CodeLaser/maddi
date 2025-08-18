package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestInstanceOf extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
                private String s;
                C(Object o) {
                    if(o instanceof String str) {
                        this.s = str;
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo C = typeInfo.findConstructor(1);
        Expression expression = C.methodBody().statements().getFirst().expression();
        if (expression instanceof InstanceOf io) {
            assertEquals("5-12:5-34", io.source().compact2());
            DetailedSources ds = io.source().detailedSources();
            assertEquals("@5:25-5:30", ds.detail(io.testType()).toString());
            assertEquals("@5:32-5:34", ds.detail(io.patternVariable().localVariable()).toString());
        } else fail();
    }

}
