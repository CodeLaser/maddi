package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.GetSetEquivalentImpl.EMPTY;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaNetHttp extends CommonTest {

    @Test
    public void testHttpRequestNewBuilder() {
        TypeInfo typeInfo = compiledTypesManager().get(HttpRequest.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("newBuilder", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    @Test
    public void testHttpRequestNewBuilderUri() {
        TypeInfo uri = compiledTypesManager().get(URI.class);
        TypeInfo typeInfo = compiledTypesManager().get(HttpRequest.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("newBuilder", uri);

        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        MethodInfo zeroArgs = typeInfo.findUniqueMethod("newBuilder", 0);
        Value.GetSetEquivalent gse = methodInfo.analysis().getOrDefault(GET_SET_EQUIVALENT, EMPTY);
        assertSame(zeroArgs, gse.methodWithoutParameters());
    }

    @Test
    public void testHttpRequestBuilderGET() {
        TypeInfo typeInfo = compiledTypesManager().get(HttpRequest.Builder.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("GET", 0);
        testFluent(methodInfo);
        testCommutable(methodInfo);
    }

    @Test
    public void testHttpRequestBuilderUri() {
        TypeInfo typeInfo = compiledTypesManager().get(HttpRequest.Builder.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("uri", 1);
        testFluent(methodInfo);
        testCommutable(methodInfo);
    }

    @Test
    public void testHttpRequestBuilderTimeout() {
        TypeInfo typeInfo = compiledTypesManager().get(HttpRequest.Builder.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("timeout", 1);
        testFluent(methodInfo);
        testCommutable(methodInfo);
    }

    private static void testFluent(MethodInfo methodInfo) {
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(TRUE, methodInfo.analysis().getOrDefault(FLUENT_METHOD, FALSE));
        assertSame(DEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(MUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

    private void testCommutable(MethodInfo methodInfo) {
        Value.CommutableData cd = methodInfo.analysis().getOrNull(COMMUTABLE_METHODS, ValueImpl.CommutableData.class);
        assertTrue(cd.isBlankMultiParSeq());
    }
}
