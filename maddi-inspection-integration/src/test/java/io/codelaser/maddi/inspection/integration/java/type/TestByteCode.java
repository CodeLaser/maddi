/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.type;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
import io.codelaser.maddi.inspection.api.resource.Resources;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        Resources classpath = ctm.classPath();
        AtomicInteger known = new AtomicInteger();
        AtomicInteger knownJavaUtil = new AtomicInteger();
        classpath.visit(new String[0], (parts, list) -> {
            known.addAndGet(list.size());
            if (String.join(".", parts).startsWith("java.util.")) {
                knownJavaUtil.addAndGet(list.size());
            }
        });
        List<TypeInfo> loaded = ctm.typesLoaded(true);

        int inspected = 0;
        int inspectedJavaUtil = 0;
        for (TypeInfo ti : loaded) {
            if (ti.hasBeenInspected()) {
                inspected++;
                if (ti.packageName().startsWith("java.util")) {
                    inspectedJavaUtil++;
                }
            } else {
                assertTrue(ti.haveOnDemandInspection());
            }
        }
        LOGGER.info("Known {}, inspected {} loaded {}; java.util*: {}, {}", known.get(), inspected, loaded.size(),
                knownJavaUtil.get(), inspectedJavaUtil);
        assertTrue(known.get() > inspected);
        assertTrue(inspected < loaded.size());
    }

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().type(AbstractMockMvcBuilder.class,
                javaInspector.mainSources());
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
        TypeInfo typeInfo = javaInspector.compiledTypesManager().type(FileOutputStream.class);
        MethodInfo close = typeInfo.findUniqueMethod("close", 0);
        assertEquals("java.io.FileOutputStream.close()", close.fullyQualifiedName());
        assertEquals("Type java.io.IOException", close.exceptionTypes().getFirst().toString());
    }

    @Test
    public void testThrows2() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().type("java.lang.ScopedValue",
                null);
        TypeInfo carrier = typeInfo.findSubType("Carrier");
        MethodInfo close = carrier.findUniqueMethod("call", 1);
        assertEquals("java.lang.ScopedValue.Carrier.call(CallableOp)", close.fullyQualifiedName());
        assertEquals("Type param X extends Throwable", close.exceptionTypes().getFirst().toString());
    }

    @Test
    public void testLongRotateRight() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().type(Long.class);
        MethodInfo rotateRight = typeInfo.findUniqueMethod("rotateRight", 2);
        assertEquals("java.lang.Long.rotateRight(long,int)", rotateRight.fullyQualifiedName());
        assertEquals("i", rotateRight.parameters().getFirst().name());
        // automatically assigned name
        assertEquals("i1", rotateRight.parameters().get(1).name());
    }

    @Test
    public void testOverrides() {
        TypeInfo arrayList = javaInspector.compiledTypesManager().type(ArrayList.class);
        assertEquals("java.util.AbstractList<E>", arrayList.parentClass().fullyQualifiedName());
        assertEquals("""
                Type Cloneable, Type java.io.Serializable, Type java.util.List<E>, Type java.util.RandomAccess\
                """, arrayList.interfacesImplemented()
                .stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
        MethodInfo getInt = arrayList.findUniqueMethod("get", 1);
        assertEquals("java.util.ArrayList.get(int)", getInt.fullyQualifiedName());
        assertEquals("""
                java.util.AbstractList.get(int), java.util.List.get(int)\
                """, getInt.overrides().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }

    @Test
    public void testPackageContainsTypes() {
        CompiledTypesManager ct = javaInspector.compiledTypesManager();
        assertTrue(ct.packageContainsTypes("java.util"));
        assertTrue(ct.packageContainsTypes("java.util.function"));
        assertFalse(ct.packageContainsTypes("java.utility"));
    }
}
