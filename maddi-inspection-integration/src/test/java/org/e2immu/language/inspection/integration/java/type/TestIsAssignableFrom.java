package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestIsAssignableFrom extends CommonTest {

    @Test
    public void test() {
        TypeInfo closeable = javaInspector.compiledTypesManager().getOrLoad(Closeable.class);
        assertEquals("java.io.Closeable", closeable.fullyQualifiedName());
        TypeInfo iterable = javaInspector.compiledTypesManager().get(Iterable.class);
        assertEquals("java.lang.Iterable", iterable.fullyQualifiedName());
        Runtime runtime = javaInspector.runtime();
        ParameterizedType closeablePt = runtime.newParameterizedType(closeable, 0);
        ParameterizedType iterableCloseable = runtime.newParameterizedType(iterable, List.of(closeablePt));
        assertEquals("Iterable<java.io.Closeable>", iterableCloseable.fullyQualifiedName());
        ParameterizedType closeableArray = runtime.newParameterizedType(closeable, 1);
        assertEquals("java.io.Closeable[]", closeableArray.fullyQualifiedName());

        // there is no special code in IsAssignableFrom to make this work!
        // there is, however, special code in HiddenContentSelector to deal with the 'transfer'
        // from array base to type parameter
        assertFalse(iterableCloseable.isAssignableFrom(runtime, closeableArray));
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                 interface Parent<T extends Parent<?>> {
                }
                interface Child extends Parent<Child> {
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        TypeInfo parent = X.findSubType("Parent");
        TypeInfo child = X.findSubType("Child");
        assertTrue(parent.asParameterizedType().isAssignableFrom(javaInspector.runtime(), child.asParameterizedType()));

        assertEquals("rRYs3LDF1ia1MgjUQEW0Aw==", X.compilationUnit().fingerPrintOrNull().toString());
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        assertSame(MD5FingerPrint.NO_FINGERPRINT, list.compilationUnit().fingerPrintOrNull());
        assertSame(MD5FingerPrint.NO_FINGERPRINT, list.compilationUnit().sourceSet().fingerPrintOrNull());
    }
}
