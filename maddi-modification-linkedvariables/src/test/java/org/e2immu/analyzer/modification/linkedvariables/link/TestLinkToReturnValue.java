package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLinkToReturnValue extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.lang.annotation.Annotation;
            class X {
                public static void method(Annotation annotation) {
                    Class<? extends Annotation> annotationClazz = annotation.annotationType();
                }
            }
            """;


    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo clazz = javaInspector.compiledTypesManager().get(Class.class);
        assertTrue(clazz.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isImmutable());

        TypeInfo annotation = javaInspector.compiledTypesManager().getOrLoad(Annotation.class);
        assertTrue(annotation.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isImmutableHC());
        MethodInfo annotationType = annotation.findUniqueMethod("annotationType", 0);
        assertTrue(annotationType.isNonModifying());

        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement s0 = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo viAc0 = vd0.variableInfo("annotationClazz");
        // an immutable type cannot be linked to anything, but the result of linking is determined
        // by the fact that annotationType() is non-modifying and Annotation is immutable-hc.
        assertEquals("", viAc0.linkedVariables().toString());
    }
}
