package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.stream.Collector;
import java.util.stream.Stream;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE_HC;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NOT_NULL;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NULLABLE;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaUtilStream extends CommonTest {

    @Test
    public void testCollector() {
        TypeInfo typeInfo = compiledTypesManager().get(Collector.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(DEPENDENT, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertFalse(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testStream() {
        TypeInfo typeInfo = compiledTypesManager().get(Stream.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertFalse(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testStreamMap() {
        TypeInfo typeInfo = compiledTypesManager().get(Stream.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("map", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertFalse(methodInfo.isFactoryMethod());
        assertSame(TRUE, methodInfo.analysis().getOrDefault(FINALIZER_METHOD, FALSE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(MUTABLE, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertTrue(p0.isIgnoreModifications());
        assertTrue(p0.isModified());
    }

    @Test
    public void testStreamOfT() {
        TypeInfo typeInfo = compiledTypesManager().get(Stream.class);
        MethodInfo methodInfo = typeInfo.methodStream().filter(m -> "of".equals(m.name())
                && 0 == m.parameters().getFirst().parameterizedType().arrays()).findFirst().orElseThrow();
        assertEquals("java.util.stream.Stream.of(T)", methodInfo.fullyQualifiedName());
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isStatic());
        assertTrue(methodInfo.isFactoryMethod());

        assertFalse(methodInfo.isModifying());
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(IMMUTABLE_HC, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(NOT_NULL, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertFalse(p0.isModified());
        assertFalse(p0.isIgnoreModifications());
    }

    @Test
    public void testStreamEmpty() {
        TypeInfo typeInfo = compiledTypesManager().get(Stream.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("empty", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isStatic());
        assertTrue(methodInfo.isFactoryMethod());

        assertFalse(methodInfo.isModifying());
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }


    @Test
    public void testStreamFilter() {
        TypeInfo typeInfo = compiledTypesManager().get(Stream.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("filter", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertFalse(methodInfo.isFactoryMethod());

        assertTrue(methodInfo.isModifying());
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }


    @Test
    public void testStreamFindFirst() {
        TypeInfo typeInfo = compiledTypesManager().get(Stream.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("findFirst", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isStatic());
        assertFalse(methodInfo.isFactoryMethod());

        assertTrue(methodInfo.isModifying());
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }

}
