package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.e2immu.language.inspection.resource.CompiledTypesManagerImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder;

import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestByteCode extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestByteCode.class);

    public TestByteCode() {
        // true for test(), because 'org.springframework.web.servlet.DispatcherServlet' is missing
        super(true);
    }

    @Test
    public void testOnDemand() {
        CompiledTypesManager ctm = javaInspector.compiledTypesManager();
        Resources classpath = ((CompiledTypesManagerImpl) ctm).classPath();
        AtomicInteger known = new AtomicInteger();
        AtomicInteger knownJavaUtil = new AtomicInteger();
        classpath.visit(new String[0], (parts, list) -> {
            known.addAndGet(list.size());
            if(String.join(".", parts).startsWith("java.util.")) {
                knownJavaUtil.addAndGet(list.size());
            }
        });
        List<TypeInfo> loaded = ctm.typesLoaded(true);

        AtomicInteger inspected = new AtomicInteger();
        AtomicInteger inspectedJavaUtil = new AtomicInteger();
        loaded.forEach(ti -> {
            if (ti.hasBeenInspected()) {
                inspected.incrementAndGet();
                if(ti.packageName().startsWith("java.util")) {
                    inspectedJavaUtil.incrementAndGet();
                }
            } else {
                assertTrue(ti.haveOnDemandInspection());
            }
        });
        LOGGER.info("Known {}, inspected {} loaded {}; java.util*: {}, {}", known.get(), inspected.get(), loaded.size(),
                knownJavaUtil.get(), inspectedJavaUtil.get());
        assertTrue(known.get() > inspected.get());
        assertEquals(inspected.get(), loaded.size());
    }

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
