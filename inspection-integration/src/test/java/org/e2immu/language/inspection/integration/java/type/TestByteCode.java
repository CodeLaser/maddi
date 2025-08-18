package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder;

import java.io.FileOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestByteCode extends CommonTest {
    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(AbstractMockMvcBuilder.class);
        assertEquals("""
                B=TP#0 in AbstractMockMvcBuilder [Type org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder<\
                B extends org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder<B>>]\
                """, typeInfo.typeParameters().stream()
                .map(TypeParameter::toStringWithTypeBounds).collect(Collectors.joining(", ")));
        MethodInfo apply = typeInfo.findUniqueMethod("apply", 1);
        assertEquals("""
                T=TP#0 in AbstractMockMvcBuilder.apply [Type param B extends \
                org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder<B>]\
                """, apply.typeParameters().stream()
                .map(TypeParameter::toStringWithTypeBounds).collect(Collectors.joining(", ")));
    }

    @Test
    public void testThrows() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(FileOutputStream.class);
        MethodInfo close = typeInfo.findUniqueMethod("close", 0);
        assertEquals("java.io.FileOutputStream.close()", close.fullyQualifiedName());
        assertEquals("Type java.io.IOException", close.exceptionTypes().getFirst().toString());
    }

    @Test
    public void testThrows2() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad("java.lang.ScopedValue",
                null);
        TypeInfo carrier = typeInfo.findSubType("Carrier");
        MethodInfo close = carrier.findUniqueMethod("call", 1);
        assertEquals("java.lang.ScopedValue.Carrier.call(CallableOp<? extends R,X extends Throwable>)",
                close.fullyQualifiedName());
        assertEquals("Type param X extends Throwable", close.exceptionTypes().getFirst().toString());
    }

    @Test
    public void testLongRotateRight() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(Long.class);
        MethodInfo rotateRight = typeInfo.findUniqueMethod("rotateRight", 2);
        assertEquals("java.lang.Long.rotateRight(long,int)", rotateRight.fullyQualifiedName());
        assertEquals("i", rotateRight.parameters().getFirst().name());
        // automatically assigned name
        assertEquals("i1", rotateRight.parameters().get(1).name());
    }

    @Test
    public void testOnDemandInspection() {
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        assertTrue(stream.parentClass().isJavaLangObject());
        MethodInfo flatMapToDouble = stream.findUniqueMethod("flatMapToDouble", 1);
        TypeInfo doubleStream = flatMapToDouble.returnType().bestTypeInfo();
        // parent of doubleStream is not yet known; the type was not loaded because it is not in the hierarchy of Stream
        assertTrue(doubleStream.haveOnDemandInspection());
        // asking for it triggers on-demand inspection
        assertTrue(doubleStream.parentClass().isJavaLangObject());
        // afterwards, on demand inspection for this type is deactivated
        assertFalse(doubleStream.haveOnDemandInspection());
    }

    @Test
    public void testPackageContainsTypes() {
        CompiledTypesManager ct = javaInspector.compiledTypesManager();
        assertTrue(ct.packageContainsTypes("java.util"));
        assertTrue(ct.packageContainsTypes("java.util.function"));
        assertFalse(ct.packageContainsTypes("java.utility"));
    }
}
