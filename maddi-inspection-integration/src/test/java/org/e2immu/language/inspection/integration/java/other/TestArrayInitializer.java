package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.ClassExpression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestArrayInitializer extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public String[] names = { java.util.List.class.getName() };
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        FieldInfo names = X.getFieldByName("names", true);
        ArrayInitializer ai = (ArrayInitializer) names.initializer();
        MethodCall mc = (MethodCall) ai.expressions().getFirst();
        ClassExpression classExpression = (ClassExpression) mc.object();
        assertEquals("Type Class<java.util.List>", classExpression.parameterizedType().toString());
        ParameterizedType pt = classExpression.type();
        assertEquals("Type java.util.List", pt.toString());
        assertEquals("3-31:3-44", classExpression.source().detailedSources().detail(pt).compact2());
        //noinspection ALL
        List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                classExpression.source().detailedSources().associatedObject(pt.typeInfo());
        // there are no qualification objects, so the associated object is not present
        assertNull(tis);
        // but we do have a package string
        assertEquals("3-31:3-39", classExpression.source().detailedSources()
                .detail(pt.typeInfo().packageName()).compact2());
    }

}
