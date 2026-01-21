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

package org.e2immu.analyzer.modification.io;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.io.LinkCodec;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
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
             {"name": "MgetI(0)", "data":{"getSetField":["Fi(1)",false,false],"nonModifyingMethod":1}},
             {"name": "MgetN(1)", "data":{"getSetField":["Fn(0)",false,false],"nonModifyingMethod":1}}]}
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
        Codec codec = new LinkCodec(runtime, javaInspector.mainSources()).codec();
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
              {"name": "Mset(0)", "data":{"getSetField":["Fset(0)",false,false]}},
              {"name": "Mi(1)", "data":{"getSetField":["Fi(1)",false,false]}},
              {"name": "Mlist(2)", "data":{"getSetField":["Flist(2)",false,false]}}]},
             {"name": "MsetAdd(0)", "data":{"nonModifyingMethod":1}, "sub":
              {"name": "Pr(0)", "data":{"downcastParameter":[]}}},
             {"name": "Mmethod(1)", "data":{"nonModifyingMethod":1}}]}
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
        Codec codec = new LinkCodec(runtime, javaInspector.mainSources()).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABX.json").toPath());
        assertEquals(JSON2, written);
    }
}
