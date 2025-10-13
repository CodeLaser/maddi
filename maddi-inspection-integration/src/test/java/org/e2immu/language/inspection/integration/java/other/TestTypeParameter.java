/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.invoke.TypeDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestTypeParameter extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;
            import org.e2immu.annotation.eventual.Only;
            import org.e2immu.support.Freezable;
            
            import java.util.*;
            import java.util.function.BiConsumer;
            
            public class TypeParameter_0 <T> extends Freezable {
            
            private static class Node<S> {
                List<S> dependsOn;
                final S t;
            
                private Node(S t) {
                    this.t = t;
                }
            
                private Node(S t, List<S> dependsOn) {
                    this.t = t;
                    this.dependsOn = dependsOn;
                }
            }
            
                @NotModified(after = "frozen")
                private final Map<T, Node<T>> nodeMap = new HashMap<>();
            
                @NotModified
                public int size() {
                    return nodeMap.size();
                }
            
                @NotModified
                public boolean isEmpty() {
                    return nodeMap.isEmpty();
                }
                @NotNull
                @Modified
                @Only(before = "frozen")
                private Node<T> getOrCreate(@NotNull T t) {
                    ensureNotFrozen();
                    Objects.requireNonNull(t);
                    Node<T> node = nodeMap.get(t);
                    if (node == null) {
                        node = new Node<>(t);
                        nodeMap.put(t, node);
                    }
                    return node;
                }
            
                @NotModified(contract = true)
                public void visit(@NotNull BiConsumer<T, List<T>> consumer) {
                    nodeMap.values().forEach(n -> consumer.accept(n.t, n.dependsOn));
                }
            
                @Only(before = "frozen")
                @Modified
                public void addNode(@NotNull @NotModified T t, @NotNull(content = true) Collection<T> dependsOn, boolean bidirectional) {
                    ensureNotFrozen();
                    Node<T> node = getOrCreate(t);
                    for (T d : dependsOn) {
                        if (node.dependsOn == null) node.dependsOn = new LinkedList<>();
                        node.dependsOn.add(d);
                        if (bidirectional) {
                            Node<T> n = getOrCreate(d);
                            if (n.dependsOn == null) n.dependsOn = new LinkedList<>();
                            n.dependsOn.add(t);
                        }
                    }
                }
            
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertFalse(typeInfo.hasImplicitParent());
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;
            import org.e2immu.annotation.eventual.Only;
            import org.e2immu.support.Freezable;
            
            import java.util.*;
            
            /*
            type parameter has a bit of a weird name, to facilitate debugging.
            T as a name is too common, and processing of byte code will also go through ParameterizedTypeFactory.
             */
            public class TypeParameter_1<TP0> extends Freezable {
            
                private static class Node<TP0> {
                    List<TP0> dependsOn;
                    final TP0 t;
            
                    private Node(TP0 t) {
                        this.t = t;
                    }
            
                    private Node(TP0 t, List<TP0> dependsOn) {
                        this.t = t;
                        this.dependsOn = dependsOn;
                    }
                }
            
                @NotModified(after = "frozen")
                private final Map<TP0, Node<TP0>> nodeMap = new HashMap<>();
            
                @NotNull
                @Modified
                @Only(before = "frozen")
                private Node<TP0> getOrCreate(@NotNull TP0 t) {
                    ensureNotFrozen();
                    Objects.requireNonNull(t);
                    Node<TP0> node = nodeMap.get(t);
                    if (node == null) {
                        node = new Node<>(t);
                        nodeMap.put(t, node);
                    }
                    return node;
                }
            
                @Only(before = "frozen")
                @Modified
                public void addNode(@NotNull @NotModified TP0 t, @NotNull(content = true) Collection<TP0> dependsOn, boolean bidirectional) {
                    for (TP0 d : dependsOn) {
                        if (bidirectional) {
                            Node<TP0> n = getOrCreate(d);
                            if (n.dependsOn == null) n.dependsOn = new LinkedList<>();
                            n.dependsOn.add(t);
                        }
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.lang.reflect.Array;
            
            public class TypeParameter_2 {
            
                static class WithId implements Cloneable {
                    int id;
            
                    @Override
                    public Object clone() {
                        try {
                            return super.clone();
                        } catch (CloneNotSupportedException ignore) {
                            throw new RuntimeException();
                        }
                    }
                }
            
                public static <T extends WithId> T[] method(T[] withIds) {
                    T[] result = (T[]) Array.newInstance(withIds.getClass().getComponentType(), withIds.length);
                    for (int i = 0;i < withIds.length;++i) {
                        if (withIds[i] != null) {
                            result[i] = (T) withIds[i].clone();
                            result[i].id = 4;
                        }
                    }
                    return result;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.io.Serializable;
            
            public class TypeParameter_4 {
                static class QName implements Serializable {
                    String localPart;
                    String getLocalPart() {
                        return localPart;
                    }
                }
                static class JAXBElement<T> implements Serializable {
                    T value;
                    QName name;
                    public T getValue() {
                        return value;
                    }
                    public QName getName() { return name; }
                }
            
                public String method(Object object, String name) {
                    if (object instanceof JAXBElement && name.equalsIgnoreCase(((JAXBElement<?>) object).getName().getLocalPart())) {
                        return ((JAXBElement<?>) object).getValue().toString();
                    }
                    return "?";
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            
            public class TypeParameter_5 {
            
                interface Base {
                    long getId();
                }
            
                static class MyHashMap<T> extends HashMap<Long, T> {
                    @Override
                    public T put(Long key, T value) {
                        assert key != null;
                        assert value != null;
                        return super.put(key, value);
                    }
                }
            
                static <T extends Base> MyHashMap<T> mapDataByID(T[] data) {
                    MyHashMap<T> result = new MyHashMap<>();
                    for (int i = 0; i < data.length; ++i) {
                        result.put(data[i].getId(), data[i]);
                    }
                    return result;
                }
            
                <T extends Base> void method1(T[] data) {
                    MyHashMap objById = mapDataByID(data);
                }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5);
        TypeInfo myHashMap = typeInfo.findSubType("MyHashMap");
        MethodInfo put = myHashMap.findUniqueMethod("put", 2);
        if (put.methodBody().statements().get(2) instanceof ReturnStatement rs && rs.expression() instanceof MethodCall mc) {
            assertEquals("java.util.HashMap.put(K,V)", mc.methodInfo().fullyQualifiedName());
            assertEquals("super.put(key,value)", mc.toString());
            assertTrue(mc.object() instanceof VariableExpression ve && ve.variable() instanceof This thisVar && thisVar.writeSuper());
        } else fail();
    }


    // on a method

    @Language("java")
    public static final String INPUT6 = """
            package a.b;
            import java.util.Collection;
            class X {
                public static <T extends Collection<I>, I> T method(T target, Collection<I>... collections) {
                    for (Collection<I> collection : collections) {
                        target.addAll(collection);
                    }
                    return target;
                }
            }
            """;

    @Test
    public void test6() {
        TypeInfo typeInfo = javaInspector.parse(INPUT6);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertEquals(2, method.typeParameters().size());
        TypeParameter tp0 = method.typeParameters().getFirst();
        assertEquals("T=TP#0 in X.method", tp0.toString());
        assertEquals("[Type java.util.Collection<I>]", tp0.typeBounds().toString());
        assertEquals("I=TP#1 in X.method", method.typeParameters().get(1).toString());
    }


    // on a type

    @Language("java")
    public static final String INPUT7 = """
            package a.b;
            import java.util.Collection;
            class X<T extends Collection<I>, I> {
                public T method(T target, Collection<I>... collections) {
                    for (Collection<I> collection : collections) {
                        target.addAll(collection);
                    }
                    return target;
                }
            }
            """;

    @Test
    public void test7() {
        TypeInfo typeInfo = javaInspector.parse(INPUT7);
        assertEquals(2, typeInfo.typeParameters().size());
        TypeParameter tp0 = typeInfo.typeParameters().getFirst();
        assertEquals("T=TP#0 in X", tp0.toString());
        assertEquals("[Type java.util.Collection<I>]", tp0.typeBounds().toString());
        assertEquals("I=TP#1 in X", typeInfo.typeParameters().get(1).toString());
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertEquals(0, method.typeParameters().size());
    }

    @Language("java")
    public static final String INPUT8 = """
            package a.b;
            import java.util.Collection;
            class X<T extends X<T>> {
                public T method() {
                    return null;
                }
                public static <T extends X<T>> T staticMethod(T t) { return null; }
            }
            """;

    @DisplayName("verify type parameter bounds")
    @Test
    public void test8() {
        TypeInfo typeInfo = javaInspector.parse(INPUT8);
        {
            assertEquals(1, typeInfo.typeParameters().size());
            TypeParameter tp0 = typeInfo.typeParameters().getFirst();
            assertEquals("T=TP#0 in X", tp0.toString());
            assertEquals(1, tp0.typeBounds().size());
            ParameterizedType tb = tp0.typeBounds().getFirst();
            assertEquals("Type a.b.X<T extends a.b.X<T>>", tb.toString());

            String print = javaInspector.print2(typeInfo);
            String expect = """
                    package a.b;
                    class X<T extends X<T>> {
                        public T method() { return null; }
                        public static <T extends X<T>> T staticMethod(T t) { return null; }
                    }
                    """;
            assertEquals(expect, print);
        }
        {
            TypeInfo typeDescriptor = javaInspector.compiledTypesManager().getOrLoad(TypeDescriptor.class);
            TypeInfo ofField = typeDescriptor.findSubType("OfField");
            assertEquals("Type java.lang.invoke.TypeDescriptor.OfField<F extends java.lang.invoke.TypeDescriptor.OfField<F>>",
                    ofField.asParameterizedType().toString());

            TypeInfo ofMethod = typeDescriptor.findSubType("OfMethod");
            assertEquals("""
                            Type java.lang.invoke.TypeDescriptor.OfMethod<F extends java.lang.invoke.TypeDescriptor.OfField<F>,\
                            M extends java.lang.invoke.TypeDescriptor.OfMethod<F,M>>\
                            """,
                    ofMethod.asParameterizedType().toString());
            TypeParameter tp0 = ofMethod.typeParameters().getFirst();
            assertEquals("""
                    F extends java.lang.invoke.TypeDescriptor.OfField<F>\
                    """, tp0.print(QualificationImpl.FULLY_QUALIFIED_NAMES, true).toString());
            ParameterizedType tb0 = tp0.typeBounds().getFirst();
            assertSame(ofField, tb0.typeInfo());
            ParameterizedType tb0p0 = tb0.parameters().getFirst();
            assertSame(tp0, tb0p0.typeParameter());

            TypeParameter tp1 = ofMethod.typeParameters().get(1);
            assertEquals("""
                    M extends java.lang.invoke.TypeDescriptor.OfMethod<F,M>\
                    """, tp1.print(QualificationImpl.FULLY_QUALIFIED_NAMES, true).toString());
            ParameterizedType tb1 = tp1.typeBounds().getFirst();
            assertSame(ofMethod, tb1.typeInfo());
            ParameterizedType tb1p0 = tb1.parameters().getFirst();
            assertSame(tp0, tb1p0.typeParameter());
            ParameterizedType tb1p1 = tb1.parameters().get(1);
            assertSame(tp1, tb1p1.typeParameter());
        }
        {
            TypeInfo arrays = javaInspector.compiledTypesManager().getOrLoad(Arrays.class);
            MethodInfo parallelSort = arrays.methodStream()
                    .filter(mi -> "parallelSort".equals(mi.name())
                                  && mi.parameters().size() == 3
                                  && mi.parameters().getFirst().parameterizedType().isTypeParameter()).findFirst().orElseThrow();
            assertEquals("java.util.Arrays.parallelSort(T extends Comparable<? super T>[],int,int)",
                    parallelSort.fullyQualifiedName());
            TypeParameter tp0 = parallelSort.typeParameters().getFirst();

            ParameterizedType pt0 = parallelSort.parameters().getFirst().parameterizedType();
            assertSame(tp0, pt0.typeParameter());

            ParameterizedType tb0 = tp0.typeBounds().getFirst();
            assertEquals("java.lang.Comparable", tb0.typeInfo().fullyQualifiedName());
            ParameterizedType tb0p0 = tb0.parameters().getFirst();
            assertSame(tp0, tb0p0.typeParameter());
        }
    }


    @Language("java")
    public static final String INPUT9 = """
            package a;
            import java.util.List;
            import java.util.Map;
            
            interface A extends Map<String,B>, List<B> {
                class B {
                }
            }
            """;

    @Test
    public void test9() {
        TypeInfo A = javaInspector.parse(INPUT9, JavaInspectorImpl.DETAILED_SOURCES);
        DetailedSources ds = A.source().detailedSources();
        assertEquals("5-13:5-19", ds.detail(DetailedSources.EXTENDS).compact2());
        ParameterizedType map = A.interfacesImplemented().getFirst();
        assertEquals("5-21:5-33", ds.detail(map).compact2());
        ParameterizedType list = A.interfacesImplemented().get(1);
        assertEquals("5-36:5-42", ds.detail(list).compact2());
        ParameterizedType stringInMap = map.parameters().getFirst();
        assertEquals("5-25:5-30", ds.detail(stringInMap).compact2());
        ParameterizedType classBinMap = map.parameters().get(1);
        assertEquals("5-32:5-32", ds.detail(classBinMap).compact2());
        TypeInfo classBinMapTypeInfo = classBinMap.typeInfo();
        ParameterizedType classBinList = list.parameters().getFirst();
        assertEquals("5-41:5-41", ds.detail(classBinList).compact2());
        List<Source> asList = ds.details(classBinList);
        assertEquals(1, asList.size());
        assertSame(asList.getFirst(), ds.detail(classBinList));
        TypeInfo classBinListTypeInfo = list.parameters().getFirst().typeInfo();
        assertSame(classBinListTypeInfo, classBinMapTypeInfo);
        List<Source> detailsOfB = ds.details(classBinListTypeInfo);
        assertEquals("5-32:5-32,5-41:5-41",
                detailsOfB.stream().map(Source::compact2).collect(Collectors.joining(",")));
        assertTrue(ds.details(A).isEmpty());
    }



    @Language("java")
    public static final String INPUT10 = """
            package a;
            class X {
                interface A { char getCode(); }
                interface B { String getComment(); }
                class C<T extends A & B> {
                    T t;
                    String method() {
                        return t.getCode() + " = " + t.getComment();
                    }
                }
            }
            """;

    @Test
    public void test10() {
        TypeInfo X = javaInspector.parse(INPUT10, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo C = X.findSubType("C");
        TypeParameter T = C.typeParameters().getFirst();
        assertEquals("T=TP#0 in C [Type a.X.A, Type a.X.B]", T.toStringWithTypeBounds());
    }

}
