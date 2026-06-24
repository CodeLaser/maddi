package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ClassExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestPreload extends CommonTest {
    @Test
    public void test() {
        scan("a.b.X", "package a.b; class X { }");
        TypeInfo functionalInterface = classSymbolScanner.getType("java.lang.FunctionalInterface");
        assertNotNull(functionalInterface);
        assertEquals(3, functionalInterface.annotations().size());
        assertEquals("@Documented", functionalInterface.annotations().getFirst().toString());
        assertEquals("@Retention(RetentionPolicy.RUNTIME)", functionalInterface.annotations().get(1).toString());
        assertEquals("@Target({ElementType.TYPE})", functionalInterface.annotations().get(2).toString());
        assertEquals("Type java.lang.annotation.Annotation",
                functionalInterface.interfacesImplemented().getFirst().toString());
    }

    @Test
    public void testClassValuedAnnotation() {
        // reference ExtendWith so it is loaded from its class file (getType only returns already-loaded types)
        scan("a.b.X", "package a.b; class X { org.junit.jupiter.api.extension.ExtendWith e; }");
        // @ExtendWith is meta-annotated with @Repeatable(Extensions.class): a Class-valued annotation element,
        // loaded from the class file, must become a ClassExpression referencing Extensions
        TypeInfo extendWith = classSymbolScanner.getType("org.junit.jupiter.api.extension.ExtendWith");
        assertNotNull(extendWith);
        AnnotationExpression repeatable = extendWith.annotations().stream()
                .filter(a -> "java.lang.annotation.Repeatable".equals(a.typeInfo().fullyQualifiedName()))
                .findFirst().orElseThrow();
        Expression value = repeatable.keyValuePairs().getFirst().value();
        ClassExpression classExpression = assertInstanceOf(ClassExpression.class, value);
        assertEquals("org.junit.jupiter.api.extension.Extensions",
                classExpression.type().typeInfo().fullyQualifiedName());
    }
}
