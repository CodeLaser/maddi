/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.bytecode.java.asm;


import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.cst.impl.type.DiamondEnum;
import org.e2immu.language.cst.impl.type.ParameterizedTypePrinter;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseGenerics extends CommonJmodBaseTests {

    @Test
    public void testNormalTypeParameter() {
        TypeInfo typeInfo = compiledTypesManager.getOrLoad(Spliterator.class);
        assertEquals("java.util.Spliterator<T>", typeInfo.asParameterizedType()
                .printForMethodFQN(false, DiamondEnum.SHOW_ALL));
        assertEquals("java.util.Spliterator<>", typeInfo.asParameterizedType()
                .printForMethodFQN(false, DiamondEnum.YES));
        assertEquals("java.util.Spliterator", typeInfo.asParameterizedType()
                .printForMethodFQN(false, DiamondEnum.NO));
    }

    @Test
    public void testWildcard() {
        TypeInfo typeInfo = compiledTypesManager.getOrLoad(Collection.class);
        assertEquals("java.util.Collection<E>", typeInfo.asParameterizedType()
                .printForMethodFQN(false, DiamondEnum.SHOW_ALL));
        MethodInfo containsAll = typeInfo.methods().stream()
                .filter(m -> m.name().equals("containsAll")).findFirst().orElseThrow();
        assertEquals("java.util.Collection.containsAll(java.util.Collection<?>)", containsAll.fullyQualifiedName());
    }

    @Test
    public void testExtends1() {
        TypeInfo typeInfo = compiledTypesManager.getOrLoad(Collection.class);
        MethodInfo addAll = typeInfo.methods().stream().filter(m -> m.name().equals("addAll")).findFirst().orElseThrow();
        assertEquals("java.util.Collection.addAll(java.util.Collection<? extends E>)", addAll.fullyQualifiedName());
    }

    @Test
    public void testExtends2() {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("java.util").build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "EnumMap");

        String signature = "<K:Ljava/lang/Enum<TK;>;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/io/Serializable;Ljava/lang/Cloneable;";
        ParseGenerics<TypeInfo> parseGenerics = new ParseGenerics<>(runtime,
                new ByteCodeInspectorImpl.TypeParameterContextImpl(), compiledTypesManager.javaBase(), null,
                typeInfo, byteCodeInspector, LocalTypeMap.LoadMode.NOW, runtime::newTypeParameter,
                typeInfo.builder()::addOrSetTypeParameter, signature, false);
        int expected = "<K:Ljava/lang/Enum<TK;>;V:Ljava/lang/Object;>".length();
        int pos = parseGenerics.goReturnEndPos() + 1;
        assertEquals(expected, pos);

        TypeParameter K = typeInfo.typeParameters().getFirst();
        assertEquals(1, K.typeBounds().size());
        ParameterizedType typeBoundK = K.typeBounds().getFirst();
        assertNull(typeBoundK.wildcard());

        assertEquals("Enum<K>", ParameterizedTypePrinter.print(
                QualificationImpl.FULLY_QUALIFIED_NAMES, typeBoundK, false, DiamondEnum.SHOW_ALL,
                false, false).toString());
        assertSame(K, typeBoundK.parameters().getFirst().typeParameter());

        ParameterizedType pt = typeInfo.asParameterizedType();
        assertEquals("java.util.EnumMap<K extends Enum<K>,V>",
                pt.printForMethodFQN(false, DiamondEnum.SHOW_ALL));
    }

    @Test
    public void testSuper() {
        TypeInfo sortedSet = compiledTypesManager.getOrLoad(SortedSet.class);
        MethodInfo comparator = sortedSet.methods().stream().filter(m -> m.name().equals("comparator"))
                .findFirst().orElseThrow();
        assertEquals("java.util.Comparator<? super E>",
                comparator.returnType().printForMethodFQN(false, DiamondEnum.SHOW_ALL));
    }

    /*
      <T:Ljava/lang/Object;T_CONS:Ljava/lang/Object;T_SPLITR::Ljava/util/Spliterator$OfPrimitive<TT;TT_CONS;TT_SPLITR;>;>Ljava/lang/Object;Ljava/util/Spliterator<TT;>;

     The double colon indicates that there is no extension for a class, but there is one for an interface (OfPrimitive is a sub-interface of Spliterator)
     */
    @Test
    public void testExtends3() {
        TypeInfo typeInfo = compiledTypesManager.getOrLoad(Spliterator.OfPrimitive.class);
        ParameterizedType pt = typeInfo.asParameterizedType();
        String fqn = "java.util.Spliterator.OfPrimitive<T,T_CONS,T_SPLITR extends java.util.Spliterator.OfPrimitive<T,T_CONS,T_SPLITR>>";
        assertEquals("Type " + fqn, pt.toString());

        TypeParameter splitter = typeInfo.typeParameters().get(2);
        ParameterizedType typeBoundSplitter = splitter.typeBounds().getFirst();
        assertNull(typeBoundSplitter.wildcard());

        assertSame(splitter, typeBoundSplitter.parameters().get(2).typeParameter());

        assertEquals(fqn, pt.printForMethodFQN(false, DiamondEnum.SHOW_ALL));
    }

    @Test
    public void testExtends4() {
        TypeInfo SliceSpliterator = compiledTypesManager
                .getOrLoad("java.util.stream.StreamSpliterators.SliceSpliterator",
                        compiledTypesManager.javaBase());
        assertEquals(2, SliceSpliterator.typeParameters().size());

        TypeInfo OfPrimitive = SliceSpliterator.findSubType("OfPrimitive");
        /*
        String signature = """
                <T:Ljava/lang/Object;\    pos = 21
                T_SPLITR::Ljava/util/Spliterator$OfPrimitive<TT;TT_CONS;TT_SPLITR;>;\
                T_CONS:Ljava/lang/Object;>\
                Ljava/util/stream/StreamSpliterators$SliceSpliterator<TT;TT_SPLITR;>;\
                Ljava/util/Spliterator$OfPrimitive<TT;TT_CONS;TT_SPLITR;>;
                """;
        */
        // not 6!
        assertEquals(3, OfPrimitive.typeParameters().size());
    }

    @Test
    public void testGenericsAbstractClassLoaderValue() {
        ByteCodeInspector.TypeParameterContext context = new ByteCodeInspectorImpl.TypeParameterContextImpl();
        TypeInfo typeInfo = runtime.newTypeInfo(runtime.newCompilationUnitBuilder().
                setPackageName("jdk.internal.loader")
                .build(), "AbstractClassLoaderValue");
        TypeParameter tp1 = runtime.newTypeParameter(0, "V", typeInfo);
        tp1.builder().commit();
        context.add(tp1);
        TypeParameter tp2 = runtime.newTypeParameter(1, "CLV", typeInfo);
        tp2.builder().commit();
        context.add(tp2);

        SourceSet sourceSet = new SourceSetImpl("test", List.of(), URI.create("file:test"),
                StandardCharsets.UTF_8, false, true, true, false, false,
                Set.of(), Set.of());
        SourceFile sourceFile = new SourceFile("test", URI.create("file:/"), sourceSet, null);
        compiledTypesManager.addTypeInfo(sourceFile, typeInfo);

        String signature = "<K:Ljava/lang/Object;>Ljdk/internal/loader/AbstractClassLoaderValue<Ljdk/internal/loader/AbstractClassLoaderValue<TCLV;TV;>.Sub<TK;>;TV;>;";
        ParseGenerics<TypeInfo> parseGenerics = new ParseGenerics<>(runtime, context,
                compiledTypesManager.javaBase(), null,
                typeInfo, byteCodeInspector,
                LocalTypeMap.LoadMode.NOW, runtime::newTypeParameter,
                typeInfo.builder()::addOrSetTypeParameter, signature, false);

        int expected = "<K:Ljava/lang/Object;>".length();
        int pos = parseGenerics.goReturnEndPos() + 1;
        assertEquals(expected, pos);
    }

    @Test
    public void testEnumSet() {
        TypeInfo enumSet = compiledTypesManager.getOrLoad(EnumSet.class);
        MethodInfo constructor = enumSet.findConstructor(2);
        ParameterizedType p0Type = constructor.parameters().getFirst().parameterizedType();
        assertEquals("Type Class<E extends Enum<E>>", p0Type.toString());
        ParameterizedType tp0 = p0Type.parameters().getFirst();
        assertEquals("Type param E extends Enum<E>", tp0.toString());
        assertEquals("E=TP#0 in EnumSet", tp0.typeParameter().toString());
        assertSame(enumSet.typeParameters().getFirst(), tp0.typeParameter());

        TypeInfo enumSetProxy = enumSet.findSubType("SerializationProxy");
        assertSame(enumSet, enumSetProxy.compilationUnitOrEnclosingType().getRight());
        assertEquals(1, enumSetProxy.typeParameters().size());
        assertEquals("E=TP#0 in SerializationProxy", enumSetProxy.typeParameters().getFirst().toString());

        TypeInfo enumSetProxy2 = compiledTypesManager.getOrLoad("java.util.EnumSet.SerializationProxy", null);
        assertSame(enumSet, enumSetProxy2.compilationUnitOrEnclosingType().getRight());

        assertEquals("E=TP#0 in SerializationProxy", enumSetProxy2.typeParameters().getFirst().toString());
        assertFalse(enumSetProxy2.isPublic());


        assertSame(enumSetProxy, enumSetProxy2);
    }


    // example from "call" method in picocli's CommandLine class
    @Test
    public void testExtendsIterative() {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("java.util").build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "CommandLine");
        MethodInfo mi = runtime.newMethod(typeInfo, "call", runtime.methodTypeStaticMethod());
        MethodInfo.Builder mib = mi.builder();
        String signature = "<C::Ljava/util/concurrent/Callable<TT;>;T:Ljava/lang/Object;>(TC;[Ljava/lang/String;)TT;";
        ByteCodeInspector.TypeParameterContext typeContext = new ByteCodeInspectorImpl.TypeParameterContextImpl();
        ParseGenerics<MethodInfo> parseGenerics = new ParseGenerics<>(runtime, typeContext,
                compiledTypesManager.javaBase(), null, mi,
                byteCodeInspector, LocalTypeMap.LoadMode.NOW, runtime::newTypeParameter,
                mib::addTypeParameter, signature, false);
        parseGenerics.goReturnEndPos();
        assertEquals(2, mib.typeParameters().size());
    }

    @Test
    public void testParse() {
        ByteCodeInspector.TypeParameterContext typeContext = new ByteCodeInspectorImpl.TypeParameterContextImpl();
        ParseParameterTypes ppt = new ParseParameterTypes(runtime, byteCodeInspector, LocalTypeMap.LoadMode.NOW);
        ParseParameterTypes.Result r = ppt.parseParameterTypesOfMethod(typeContext, compiledTypesManager.javaBase(),
                null,
                "(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;", false);
        assertNotNull(r);
        assertEquals(2, r.parameterTypes().size());
        assertEquals("Type String", r.returnType().toString());
    }
}
