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

package org.e2immu.analyzer.modification.link.io;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.io.DecoratorImpl;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalyzedPackageFiles;
import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysis;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.util.internal.util.Trie;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestWriteAnalysis2 extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestWriteAnalysis2.class);

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                private int n;
                private int i;
                public X(int n) { this.n = n; }
                int getI() { return i; }
                int getN() { return n; }
            }
            """;

    @Language("java")
    private static final String OUTPUT1 = """
            package a.b;
            import org.e2immu.annotation.Final;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;
            public class X {
                @Final private int n;
                @Final private int i;
                public X(int n) { this.n = n; }
                @NotModified @GetSet("i") int getI() { return i; }
                @NotModified @GetSet("n") int getN() { return n; }
            }
            """;

    @Language("json")
    private static final String JSON1 = """
            [
            {"name": "Ta.b.X", "data":{"partOfConstructionType":["C<init>(0)"]}, "subs":[
             {"name": "Fn(0)", "data":{"finalField":1}},
             {"name": "Fi(1)", "data":{"finalField":1}},
             {"name": "C<init>(0)", "data":{"methodLinks":[[],[[["P",["Ta.b.X","C<init>(0)","Pn(0)"]],[["P",["Ta.b.X","C<init>(0)","Pn(0)"]],"→",["F",["Ta.b.X","Fn(0)"],["variableExpression","5-23:5-26",["T",["Ta.b.X"]]]]]]],["T",["Ta.b.X"]]]}},
             {"name": "MgetI(0)", "data":{"getSetField":["Fi(1)",false,false],"methodLinks":[[["R",["Ta.b.X","MgetI(0)"]],[["R",["Ta.b.X","MgetI(0)"]],"←",["F",["Ta.b.X","Fi(1)"]]]],[]],"nonModifyingMethod":1}},
             {"name": "MgetN(1)", "data":{"getSetField":["Fn(0)",false,false],"methodLinks":[[["R",["Ta.b.X","MgetN(1)"]],[["R",["Ta.b.X","MgetN(1)"]],"←",["F",["Ta.b.X","Fn(0)"]]]],[]],"nonModifyingMethod":1}}]}
            ]
            """;

    @DisplayName("basics")
    @Test
    public void test1() throws IOException {
        TypeInfo X = javaInspector.parse(INPUT1);
        prepWork(X);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        lc.doPrimaryType(X);

        String s = javaInspector.print2(X, new DecoratorImpl(runtime, javaInspector.mainSources()),
                javaInspector.importComputer(4, javaInspector.mainSources()));
        assertEquals(OUTPUT1, s);
        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(X.fullyQualifiedName().split("\\."), X);
        WriteAnalysis writeAnalysis = new WriteAnalysis(runtime);
        File dest = new File("build/json");
        if (dest.mkdirs()) LOGGER.info("Created {}", dest);
        Codec codec = new LinkCodec(javaInspector).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABX.json").toPath());
        assertEquals(JSON1, written);
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.List;
            class X {
                record R(Set<Integer> set, int i, List<String> list) {}
            
                void setAdd(R r) {
                    r.set.add(r.i);
                }
            
                void method() {
                    List<String> l = new ArrayList<>();
                    Set<Integer> s = new HashSet<>();
                    R r = new R(s, 3, l);
                    setAdd(r); // at this point, s1 should have been modified, via???
                }
            }
            """;

    // FIXME PRINT want a space after int i,; too many spaces after ArrayList<>
    @Language("java")
    private static final String OUTPUT2 = """
            package a.b;
            import java.util.*;
            import org.e2immu.annotation.NotModified;
            class X {
                record R(Set<Integer> set, int i, List<String> list) { }
                @NotModified void setAdd(X.R r) { r.set.add(r.i); }
                @NotModified
                void method() {
                    List<String> l = new ArrayList<> ();
                    Set<Integer> s = new HashSet<> ();
                    X.R r = new X.R(s, 3, l);
                    setAdd(r);
                    // at this point, s1 should have been modified, via???
                }
            }
            """;

    @Language("json")
    private static final String JSON2 = """
            [
            {"name": "Ta.b.X", "data":{"partOfConstructionType":["C<init>(0)"]}, "subs":[
             {"name": "SR(0)", "data":{"partOfConstructionType":["C<init>(0)"]}, "subs":[
              {"name": "Fset(0)", "data":{"finalField":1}},
              {"name": "Fi(1)", "data":{"finalField":1}},
              {"name": "Flist(2)", "data":{"finalField":1}},
              {"name": "C<init>(0)", "data":{"methodLinks":[[],[[["P",["Ta.b.X","SR(0)","C<init>(0)","Pset(0)"]],[["F",["Tjava.util.Set","V§m","Tjava.util.concurrent.atomic.AtomicBoolean"],["variableExpression","0-0:0-0",["P",["Ta.b.X","SR(0)","C<init>(0)","Pset(0)"]]]],"≡",["F",["Tjava.util.Set","V§m","Tjava.util.concurrent.atomic.AtomicBoolean"],["variableExpression","0-0:0-0",["F",["Ta.b.X","SR(0)","Fset(0)"]]]]],[["P",["Ta.b.X","SR(0)","C<init>(0)","Pset(0)"]],"→",["F",["Ta.b.X","SR(0)","Fset(0)"]]]],[["P",["Ta.b.X","SR(0)","C<init>(0)","Pi(1)"]],[["P",["Ta.b.X","SR(0)","C<init>(0)","Pi(1)"]],"→",["F",["Ta.b.X","SR(0)","Fi(1)"]]]],[["P",["Ta.b.X","SR(0)","C<init>(0)","Plist(2)"]],[["P",["Ta.b.X","SR(0)","C<init>(0)","Plist(2)"]],"→",["F",["Ta.b.X","SR(0)","Flist(2)"]]],[["F",["Tjava.util.List","V§m","Tjava.util.concurrent.atomic.AtomicBoolean"],["variableExpression","0-0:0-0",["P",["Ta.b.X","SR(0)","C<init>(0)","Plist(2)"]]]],"≡",["F",["Tjava.util.List","V§m","Tjava.util.concurrent.atomic.AtomicBoolean"],["variableExpression","0-0:0-0",["F",["Ta.b.X","SR(0)","Flist(2)"]]]]]]],["T",["Ta.b.X","SR(0)"]]]}},
              {"name": "Mset(0)", "data":{"getSetField":["Fset(0)",false,false]}},
              {"name": "Mi(1)", "data":{"getSetField":["Fi(1)",false,false]}},
              {"name": "Mlist(2)", "data":{"getSetField":["Flist(2)",false,false]}}]},
             {"name": "MsetAdd(0)", "data":{"methodLinks":[[],[[["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]],[["F",["Ta.b.X","SR(0)","Fi(1)"],["variableExpression","10-19:10-19",["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]]]],"∈",["F",["Tjava.util.Set","V§$s",["Tjava.lang.Integer",1,[]]],["variableExpression","0-0:0-0",["F",["Ta.b.X","SR(0)","Fset(0)"],["variableExpression","10-9:10-9",["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]]]]]]]]],["F",["Ta.b.X","SR(0)","Fset(0)"],["variableExpression","10-9:10-9",["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]]]],["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]]],"nonModifyingMethod":1}},
             {"name": "Mmethod(1)", "data":{"methodLinks":[[],[],["F",["Ta.b.X","SR(0)","Fset(0)"],["variableExpression","10-9:10-9",["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]]]],["P",["Ta.b.X","MsetAdd(0)","Pr(0)"]]],"nonModifyingMethod":1}}]}
            ]
            """;

    @DisplayName("analyzer info")
    @Test
    public void test2() throws IOException {
        TypeInfo X = javaInspector.parse(INPUT2);
        prepWork(X);
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        MethodInfo setAdd = X.findUniqueMethod("setAdd", 1);
        MethodLinkedVariables mlvSetAdd = setAdd.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(setAdd));
        assertEquals("[0:r*.i∈0:r.set*.§$s] --> -", mlvSetAdd.toString());
        assertEquals("a.b.X.setAdd(a.b.X.R):0:r, r.set", mlvSetAdd.sortedModifiedString());

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(method));
        assertEquals("a.b.X.setAdd(a.b.X.R):0:r, r.set", mlvMethod.sortedModifiedString());
        assertTrue(method.isNonModifying());

        String s = javaInspector.print2(X, new DecoratorImpl(runtime, javaInspector.mainSources()),
                javaInspector.importComputer(4, javaInspector.mainSources()));
        assertEquals(OUTPUT2, s);
        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(X.fullyQualifiedName().split("\\."), X);
        WriteAnalysis writeAnalysis = new WriteAnalysis(runtime);
        File dest = new File("build/json");
        if (dest.mkdirs()) LOGGER.info("Created {}", dest);
        Codec codec = new LinkCodec(javaInspector).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABX.json").toPath());
        // assertEquals(JSON2, written);

        javaInspector.invalidateAllSources();
        TypeInfo X2 = javaInspector.parse(INPUT2);
        LoadAnalyzedPackageFiles load = new LoadAnalyzedPackageFiles(javaInspector.mainSources());
        load.go(codec, written);

        MethodInfo setAdd2 = X2.findUniqueMethod("setAdd", 1);
        MethodLinkedVariables mlvSetAdd2 = setAdd2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:r*.i∈0:r.set*.§$s] --> -", mlvSetAdd2.toString());
        assertEquals(mlvSetAdd, mlvSetAdd2);

        assertEquals(JSON2, written);
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Map;
            import java.util.HashMap;
            import java.util.Set;
            import java.util.stream.Collectors;
            public class C<K, V> {
                Map<K, V> map;
            
                C(Map<K, V> map) { this.map = map; }
            
                private C<V, K> reverse() {
                    Map<V, K> map = new HashMap<>();
                    for(Map.Entry<K, V> entry: this.map.entrySet()) {
                        map.put(entry.getValue(), entry.getKey());
                    }
                    return new C<>(map);
                }
            }
            """;

    @Test
    public void test3() throws IOException {
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo reverse = C.findUniqueMethod("reverse", 0);
        MethodLinkedVariables mlvReverse0 = reverse.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse));
        testLink3(mlvReverse0);

        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(C.fullyQualifiedName().split("\\."), C);
        WriteAnalysis writeAnalysis = new WriteAnalysis(runtime);
        File dest = new File("build/json");
        if (dest.mkdirs()) LOGGER.info("Created {}", dest);
        Codec codec = new LinkCodec(javaInspector).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABC.json").toPath());
        LOGGER.info("Wrote {}", written);

        javaInspector.invalidateAllSources();
        TypeInfo CC = javaInspector.parse(INPUT3);
        LoadAnalyzedPackageFiles load = new LoadAnalyzedPackageFiles(javaInspector.mainSources());
        load.go(codec, written);

        MethodInfo reverseCC = CC.findUniqueMethod("reverse", 0);
        MethodLinkedVariables mlvReverseCC = reverseCC.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(mlvReverse0, mlvReverseCC);
        testLink3(mlvReverseCC);
    }

    private void testLink3(MethodLinkedVariables mlv) {
        String expected = """
                [] --> reverse.map.§vks[-1]∩this.map.§kvs,\
                reverse.map.§vks[-2]∩this.map.§kvs,reverse.map.§vks~this.map.§kvs\
                """;
        assertEquals(expected, mlv.toString());

        DependentVariable vksSlice = (DependentVariable) mlv.ofReturnValue().link(0).from();
        assertEquals("Type param V[]", vksSlice.parameterizedType().toString());
        FieldReference vks = (FieldReference) vksSlice.arrayVariable();
        assertEquals("§vks", vks.fieldInfo().name());
        assertEquals("java.util.Map", vks.fieldInfo().owner().fullyQualifiedName());
        assertEquals("Type java.util.Map.§VK[]", vks.parameterizedType().toString());
        TypeInfo vk = vks.parameterizedType().typeInfo();
        assertTrue(Util.isContainerType(vk));
        assertEquals(2, vk.fields().size());
        assertEquals("java.util.Map", vk.compilationUnitOrEnclosingType().getRight().fullyQualifiedName());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Stream;
            public class C<K, V> {
                public Map.Entry<Stream<K>, Stream<V>> oneInstance(K x, V y) {
                    return new AbstractMap.SimpleEntry<>(Stream.of(x), Stream.of(y));
                }
            }
            """;

    @Test
    public void test4() throws IOException {
        TypeInfo C = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        MethodLinkedVariables mlvOne = oneInstance.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneInstance));

        testLink4(mlvOne);

        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(C.fullyQualifiedName().split("\\."), C);
        WriteAnalysis writeAnalysis = new WriteAnalysis(runtime);
        File dest = new File("build/json");
        if (dest.mkdirs()) LOGGER.info("Created {}", dest);
        Codec codec = new LinkCodec(javaInspector).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABC.json").toPath());

        LOGGER.info("Encoded: {}", written);

        javaInspector.invalidateAllSources();
        TypeInfo CC = javaInspector.parse(INPUT4);
        LoadAnalyzedPackageFiles load = new LoadAnalyzedPackageFiles(javaInspector.mainSources());
        load.go(codec, written);


        MethodInfo oneCC = CC.findUniqueMethod("oneInstance", 2);
        MethodLinkedVariables mlvOneCC = oneCC.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(mlvOne, mlvOneCC);
        testLink4(mlvOneCC);
    }

    private void testLink4(MethodLinkedVariables mlvOne) {
        assertEquals("""
                [-, -] --> oneInstance.§ksvs.§ks∋0:x,oneInstance.§ksvs.§vs∋1:y\
                """, mlvOne.toString());

        FieldReference ks = (FieldReference) mlvOne.ofReturnValue().link(0).from();
        assertEquals("§ks", ks.fieldInfo().name());
        assertEquals("java.util.AbstractMap.SimpleEntry.§KSVS", ks.fieldInfo().owner().fullyQualifiedName());
        assertEquals("Type param K[]", ks.parameterizedType().toString());

        FieldReference ksvs = (FieldReference) ks.scopeVariable();
        assertEquals("§ksvs", ksvs.fieldInfo().name());
        assertEquals("Type java.util.AbstractMap.SimpleEntry.§KSVS", ksvs.parameterizedType().toString());
        assertEquals("java.util.Map.Entry", ksvs.fieldInfo().owner().fullyQualifiedName());
        assertTrue(Util.isContainerType(ksvs.parameterizedType().typeInfo()));
    }
}
