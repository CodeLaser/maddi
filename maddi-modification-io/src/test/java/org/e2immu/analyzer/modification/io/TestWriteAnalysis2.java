package org.e2immu.analyzer.modification.io;

import org.e2immu.analyzer.modification.linkedvariables.io.LinkedVariablesCodec;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.Info;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            {"name": "Ta.b.X", "data":{"hc":{"E":true},"partOfConstructionType":["C<init>(0)"]}, "subs":[
             {"name": "Fn(0)", "data":{"finalField":1}},
             {"name": "Fi(1)", "data":{"finalField":1}},
             {"name": "C<init>(0)", "data":{"hc":{"0":"Tjava.lang.Object"},"staticValuesMethod":["",[],[["F",["Ta.b.X","Fn(0)"],["variableExpression","5-23:5-33",["T",["Ta.b.X"]]]],["variableExpression","5-32:5-32",["P",["Ta.b.X","C<init>(0)","Pn(0)"]]]]]}, "sub":
              {"name": "Pn(0)", "data":{"parameterAssignedToField":["Fn(0)"],"staticValuesParameter":["",["variableExpression","5-32:5-32",["F",["Ta.b.X","Fn(0)"],["variableExpression","5-23:5-26",["T",["Ta.b.X"]]]]],[]]}}},
             {"name": "MgetI(0)", "data":{"getSetField":["Fi(1)",false],"hc":{},"nonModifyingMethod":1,"staticValuesMethod":["",["variableExpression","6-25:6-25",["F",["Ta.b.X","Fi(1)"]]],[]]}},
             {"name": "MgetN(1)", "data":{"getSetField":["Fn(0)",false],"hc":{},"nonModifyingMethod":1,"staticValuesMethod":["",["variableExpression","7-25:7-25",["F",["Ta.b.X","Fn(0)"]]],[]]}}]}
            ]
            """;

    @DisplayName("basics")
    @Test
    public void test1() throws IOException {
        test(INPUT1, OUTPUT1, JSON1);
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
            {"name": "Ta.b.X", "data":{"hc":{"E":true},"partOfConstructionType":["C<init>(0)"]}, "subs":[
             {"name": "SR(0)", "data":{"hc":{"0":"Tjava.util.Set","1":"Tjava.util.List","M":2},"partOfConstructionType":["C<init>(0)"]}, "subs":[
              {"name": "Fset(0)", "data":{"finalField":1}},
              {"name": "Fi(1)", "data":{"finalField":1}},
              {"name": "Flist(2)", "data":{"finalField":1}},
              {"name": "C<init>(0)", "data":{"hc":{"2":"Tjava.lang.Object","M":2},"hcsMethod":{"0":[[0]],"1":[[1]]},"staticValuesMethod":["",[],[["F",["Ta.b.X","SR(0)","Fi(1)"],["variableExpression","7-32:7-36",["T",["Ta.b.X","SR(0)"]]]],["variableExpression","7-32:7-36",["P",["Ta.b.X","SR(0)","C<init>(0)","Pi(1)"]]],["F",["Ta.b.X","SR(0)","Flist(2)"],["variableExpression","7-39:7-55",["T",["Ta.b.X","SR(0)"]]]],["variableExpression","7-39:7-55",["P",["Ta.b.X","SR(0)","C<init>(0)","Plist(2)"]]],["F",["Ta.b.X","SR(0)","Fset(0)"],["variableExpression","7-14:7-29",["T",["Ta.b.X","SR(0)"]]]],["variableExpression","7-14:7-29",["P",["Ta.b.X","SR(0)","C<init>(0)","Pset(0)"]]]]]}, "subs":[
               {"name": "Pset(0)", "data":{"hcsParameter":{"0":[[-1]]},"parameterAssignedToField":["Fset(0)"],"staticValuesParameter":["",["variableExpression","7-14:7-29",["F",["Ta.b.X","SR(0)","Fset(0)"]]],[]]}},
               {"name": "Pi(1)", "data":{"parameterAssignedToField":["Fi(1)"],"staticValuesParameter":["",["variableExpression","7-32:7-36",["F",["Ta.b.X","SR(0)","Fi(1)"]]],[]]}},
               {"name": "Plist(2)", "data":{"hcsParameter":{"1":[[-1]]},"parameterAssignedToField":["Flist(2)"],"staticValuesParameter":["",["variableExpression","7-39:7-55",["F",["Ta.b.X","SR(0)","Flist(2)"]]],[]]}}]},
              {"name": "Mset(0)", "data":{"getSetField":["Fset(0)",false],"hc":{"M":2},"hcsMethod":{"0":[[-1]]},"nonModifyingMethod":1,"staticValuesMethod":["",["variableExpression","7-14:7-29",["F",["Ta.b.X","SR(0)","Fset(0)"]]],[]]}},
              {"name": "Mi(1)", "data":{"getSetField":["Fi(1)",false],"hc":{"M":2},"nonModifyingMethod":1,"staticValuesMethod":["",["variableExpression","7-32:7-36",["F",["Ta.b.X","SR(0)","Fi(1)"]]],[]]}},
              {"name": "Mlist(2)", "data":{"getSetField":["Flist(2)",false],"hc":{"M":2},"hcsMethod":{"1":[[-1]]},"nonModifyingMethod":1,"staticValuesMethod":["",["variableExpression","7-39:7-55",["F",["Ta.b.X","SR(0)","Flist(2)"]]],[]]}}]},
             {"name": "C<init>(0)", "data":{"hc":{"0":"Tjava.lang.Object"}}},
             {"name": "MsetAdd(0)", "data":{"hc":{"0":"Ta.b.X.R"},"nonModifyingMethod":1}, "sub":
              {"name": "Pr(0)", "data":{"hcsParameter":{"0":[[-1]]},"modifiedComponentsParameter":[["F",["Ta.b.X","SR(0)","Fset(0)"],["variableExpression","10-9:10-9",["T",["Ta.b.X","SR(0)"]]]],true]}}},
             {"name": "Mmethod(1)", "data":{"hc":{}}}]}
            ]
            """;

    @DisplayName("analyzer info")
    @Test
    public void test2() throws IOException {
        test(INPUT2, OUTPUT2, JSON2);
    }

    private void test(String input, String output, String json) throws IOException {
        TypeInfo X = javaInspector.parse(input);
        List<Info> analysisOrder = prepWork(X);
        modAnalyzer.go(analysisOrder);

        String s = javaInspector.print2(X, new DecoratorImpl(runtime), javaInspector.importComputer(4,
                javaInspector.mainSources()));
        assertEquals(output, s);
        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(X.fullyQualifiedName().split("\\."), X);
        WriteAnalysis writeAnalysis = new WriteAnalysis(runtime);
        File dest = new File("build/json");
        if(dest.mkdirs()) LOGGER.info("Created {}", dest);
        Codec codec = new LinkedVariablesCodec(runtime).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABX.json").toPath());
        assertEquals(json, written);
    }
}
