package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestForEachLambda extends CommonTest {

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class X {
                 interface I { }
                 class II implements I { }
                 Set<II> set = new HashSet<>();
                 void add(II ii) {
                     set.add(ii);
                 }
                 void add2(II ii) {
                     add(ii);
                 }
                 void method(List<II> list) {
                    list.forEach(j -> add(j));
                 }
            }
            """;

    @DisplayName("list.forEach(j -> add(j))")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo add = X.findUniqueMethod("add", 1);
        MethodLinkedVariables mtlAdd = add.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(add));
        assertEquals("[0:ii∈this.set.§$s] --> -", mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("add2", 1);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(add2));
        assertEquals("[0:ii∈this.set.§$s] --> -", add2Mtl.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~this.set.§$s", tlvT1.toString());

        assertEquals("[0:list.§$s~this.set.§$s] --> -", mlv.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class X {
                 interface I { }
                 class II implements I { }
                 Set<II> set = new HashSet<>();
                 void method(List<II> list) {
                    list.forEach(j -> set.add(j));
                 }
            }
            """;

    @DisplayName("list.forEach(j -> set.add(j))")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~this.set.§$s", tlvT1.toString());
        assertEquals("[0:list.§$s~this.set.§$s] --> -", mlv.toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class X {
                 interface I { }
                 class II implements I { }
                 Set<II> set = new HashSet<>();
                 void add(II ii) {
                     set.add(ii);
                 }
                 void method(List<II> list, X x) {
                    list.forEach(j -> x.add(j));
                 }
            }
            """;

    @DisplayName("list.forEach(j -> x.add(j))")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("add", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 2);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~1:x.set.§$s", tlvT1.toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class X {
                interface I { }
                class II implements I {
                    void method2(String s){ }
                }
                static void add(II ii) {
                    ii.method2("abc");
                }
                void method(List<II> list) {
                    list.forEach(j -> X.add(j));
                }
            }
            """;

    @DisplayName("list.forEach(j -> X.add(j))")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo II = X.findSubType("II");
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - II §0", vfc.compute(II).toString());
        MethodInfo method2 = II.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlvMethod2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        assertEquals("[] --> -", mlvMethod2.toString());

        MethodInfo add = X.findUniqueMethod("add", 1);
        MethodLinkedVariables mlvAdd = add.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(add));
        assertEquals("[-] --> -", mlvAdd.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        // we keep the link, to be able to propagate modifications/type use
        assertEquals("0:list.§$s∋0:j", tlvT1.toString());

        assertEquals("[0:list.§$s∋0:j] --> -", mlvMethod.toString());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class X {
                 interface I { }
                 class II implements I { }
                 Map<Integer, II> map = new HashMap<>();
                 void put(int i, II ii) {
                     map.put(i, ii);
                 }
                 void put2(int i, II ii) {
                     put(i, ii);
                 }
                 void method(Map<Integer, II> map) {
                    map.forEach((k, j) -> put(k, j));
                 }
                 void method2(Map<Integer, II> map) {
                    map.forEach((k, j) -> this.map.put(k, j));
                 }
            }
            """;

    @DisplayName("map.forEach((k, j) -> put(k, j))")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo add = X.findUniqueMethod("put", 2);
        tlc.doPrimaryType(X);
        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:i∈this.map.§$$s[-1].§$, 1:ii∈this.map.§$$s[-2].§$] --> -", mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("put2", 2);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("[0:i∈this.map.§$$s[-1].§$, 1:ii∈this.map.§$$s[-2].§$] --> -", add2Mtl.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo map = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(map.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:map.§$$s~this.map.§$$s[-2].§$,0:map.§$$s~this.map.§$$s[-1].§$", tlvT1.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlv2 = method2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:map.§$$s~this.map.§$$s[-2].§$,0:map.§$$s~this.map.§$$s[-1].§$] --> -",
                mlv2.toString());
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class X {
                 interface H { }
                 interface I { }
                 class II implements I { }
                 Map<H, II> map = new HashMap<>();
                 void put(H h, II ii) {
                     map.put(h, ii);
                 }
                 void put2(H h, II ii) {
                     put(h, ii);
                 }
                 static void method(Map<H, II> map) {
                    map.forEach((p0, p1) -> put(p0, p1));
                 }
                 void method2(Map<H, II> map) {
                    map.forEach((p0, p1) -> put(p0, p1));
                 }
            }
            """;

    @DisplayName("map.forEach((p0, p1) -> put(p0, p1))")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);

        final String LINKS_PUT = "[0:h∈this.map.§$$s[-1].§$, 1:ii∈this.map.§$$s[-2].§$] --> -";

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("put", 2);
        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(LINKS_PUT, mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("put2", 2);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals(LINKS_PUT, add2Mtl.toString());

        final String LINKS_MAP_PUT = "[0:h∈this.map.§$$s[-1].§$, 1:ii∈this.map.§$$s[-2].§$] --> -";

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(LINKS_MAP_PUT, mlvMethod.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlvMethod2 = method2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(LINKS_MAP_PUT, mlvMethod2.toString());
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class X {
                 interface H { }
                 interface I { }
                 class II implements I { }
                 Map<II, H> map = new HashMap<>();
                 void put(II ii, H h) {
                     map.put(ii, h);
                 }
                 void method(Map<H, II> map) {
                    map.forEach((p0, p1) -> put(p1, p0));
                 }
            }
            """;

    @DisplayName("map.forEach((p0, p1) -> put(p1, p0))")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo put = X.findUniqueMethod("put", 2);
        MethodLinkedVariables mlvPut = put.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(put));
        assertEquals("[0:ii∈this.map.§$$s[-1].§$, 1:h∈this.map.§$$s[-2].§$] --> -", mlvPut.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[0:ii∈this.map.§$$s[-1].§$, 1:h∈this.map.§$$s[-2].§$] --> -", mlvMethod.toString());
    }

    @Language("java")
    String INPUT10 = """
            package a.b.ii;
            class C {
                interface II {
                    void method1(String s);
                }
                private final II ii;
                C(II ii) { this.ii = ii; }
                void method(String string) {
                   string.chars().forEach(c -> ii.method1("ab = " + c));
                }
            }
            """;

    @DisplayName("forEach without type parameters")
    @Test
    public void test10() {
        TypeInfo C = javaInspector.parse(INPUT10);
        TypeInfo II = C.findSubType("II");

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - C §0", vfc.compute(C).toString());

        MethodInfo method1 = II.findUniqueMethod("method1", 1);
        method1.parameters().getFirst().analysis().set(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.INDEPENDENT);

        MethodInfo method = C.findUniqueMethod("method", 1);
        method.parameters().getFirst().analysis().set(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.INDEPENDENT);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
    }

}
