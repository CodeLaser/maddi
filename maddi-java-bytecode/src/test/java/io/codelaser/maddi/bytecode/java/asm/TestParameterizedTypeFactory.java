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

package io.codelaser.maddi.bytecode.java.asm;


import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class TestParameterizedTypeFactory extends CommonJmodBaseTests {


    @Test
    public void testInt() {
        assertEquals(runtime.intParameterizedType(), create("I").parameterizedType);
    }

    private ParameterizedTypeFactory.Result create(String signature) {
        return ParameterizedTypeFactory.from(runtime, compiledTypesManager.javaBase(), null,
                new ByteCodeInspectorImpl.TypeParameterContextImpl(), byteCodeInspector,
                LocalTypeMap.LoadMode.NOW, signature, false);
    }

    @Test
    public void testString() {
        ParameterizedType pt = create("[Ljava/lang/String;").parameterizedType;
        // This is an important test, the predefined type "java.lang.String" must be the same object
        // as the type that has been loaded by the bytecode inspector.
        TypeInfo string = pt.typeInfo();
        assertSame(runtime.stringTypeInfo(), string);
        assertEquals(1, pt.arrays());
        assertNotNull(string);
        assertNotNull(string.source());
        assertEquals("predefined://java/lang", string.compilationUnit().uri().toString());
    }

    @Test
    public void testMapEntry() {
        ParameterizedType pt = create("Ljava/util/Map$Entry;").parameterizedType;
        assertEquals("java.util.Map.Entry", pt.typeInfo().fullyQualifiedName());
    }

    @Test
    public void testTypeDescriptor() {
        ParameterizedType pt = create("Ljava/lang/invoke/TypeDescriptor$OfField;").parameterizedType;
        assertEquals("Type java.lang.invoke.TypeDescriptor.OfField", pt.toString());
        TypeInfo parent = pt.typeInfo().compilationUnitOrEnclosingType().getRight();
        assertEquals("java.lang.invoke.TypeDescriptor", parent.fullyQualifiedName());
        assertSame(pt.typeInfo(), parent.findSubType("OfField"));
    }

    @Test
    public void testStringAndInt() {
        String desc = "[Ljava/lang/String;I";
        ParameterizedTypeFactory.Result res = create(desc);
        assertSame(runtime.stringTypeInfo(), res.parameterizedType.typeInfo());
        assertEquals(1, res.parameterizedType.arrays());
        assertEquals('I', desc.charAt(res.nextPos));
    }

    @Test
    public void testCharArray() {
        ParameterizedType pt = create("[C").parameterizedType;
        assertSame(runtime.charTypeInfo(), pt.typeInfo());
        assertEquals(1, pt.arrays());
    }

    @Test
    public void testGenerics() {
        ParameterizedType pt = create("Ljava/util/List<Ljava/lang/String;>;").parameterizedType;
        assertEquals("java.util.List",
                Objects.requireNonNull(pt.typeInfo()).fullyQualifiedName());
        assertEquals(1, pt.parameters().size());
        ParameterizedType tp1 = pt.parameters().get(0);
        assertNotNull(tp1.typeInfo());
        assertEquals("java.lang.String", tp1.typeInfo().fullyQualifiedName());
    }


    @Test
    public void testWildcard() {
        ParameterizedType pt = create("Ljava/lang/Class<*>;").parameterizedType;
        assertNotNull(pt.typeInfo());
        assertEquals("java.lang.Class", pt.typeInfo().fullyQualifiedName());
        assertEquals(1, pt.parameters().size());
        ParameterizedType tp0 = pt.parameters().get(0);
        assertTrue(tp0.wildcard().isUnbound());
        assertNull(pt.wildcard());
    }

    @Test
    public void testWildcardSuper() {
        ParameterizedType pt = create("Ljava/lang/Class<-Ljava/lang/Number;>;").parameterizedType;
        assertEquals("java.lang.Class", pt.typeInfo().fullyQualifiedName());
        assertEquals(1, pt.parameters().size());
        ParameterizedType tp0 = pt.parameters().get(0);
        assertSame(runtime.wildcardSuper(), tp0.wildcard());
    }

    @Test
    public void testWildcardExtends() {
        ParameterizedType pt = create("Ljava/lang/Class<+Ljava/lang/Number;>;").parameterizedType;
        assertEquals("java.lang.Class", pt.typeInfo().fullyQualifiedName());
        assertEquals(1, pt.parameters().size());
        ParameterizedType tp0 = pt.parameters().get(0);
        assertSame(runtime.wildcardExtends(), tp0.wildcard());
    }

    @Test
    public void testGenerics2() {
        String signature = "Ljava/util/List<Ljava/lang/String;Ljava/lang/Integer;>;";
        ParameterizedType pt = create(signature).parameterizedType;
        assertEquals("java.util.List", pt.typeInfo().fullyQualifiedName());
        assertEquals(2, pt.parameters().size());
        ParameterizedType tp1 = pt.parameters().get(0);
        assertEquals("java.lang.String", tp1.typeInfo().fullyQualifiedName());
        ParameterizedType tp2 = pt.parameters().get(1);
        assertNotNull(tp2.typeInfo());
        assertEquals("java.lang.Integer",
                tp2.typeInfo().fullyQualifiedName());
    }

    @Test
    public void testGenericsMap() {
        String signature = "Ljava/util/List<Ljava/util/Map<Ljava/lang/String;Ljava/lang/Double;>;>;";
        ParameterizedType pt = create(signature).parameterizedType;
        assertNotNull(pt.typeInfo());
        assertEquals("java.util.List", pt.typeInfo().fullyQualifiedName());
        assertEquals(1, pt.parameters().size());
        ParameterizedType tp1 = pt.parameters().get(0);
        assertNotNull(tp1.typeInfo());
        assertEquals("java.util.Map", tp1.typeInfo().fullyQualifiedName());
        assertEquals(2, tp1.parameters().size());
        ParameterizedType p1 = tp1.parameters().get(1);
        assertNotNull(p1);
        assertEquals("java.lang.Double", p1.typeInfo().fullyQualifiedName());
    }
}
