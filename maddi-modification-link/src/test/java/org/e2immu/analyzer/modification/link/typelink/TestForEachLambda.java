package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
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
        tlc.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("add", 1);
        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[#0:e(*[Type a.b.X.II]:*[Type a.b.X.II]);set(*[Type a.b.X.II]:0[Type java.util.Set<a.b.X.II>])]",
                mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("add2", 1);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                [#0:e(*[Type a.b.X.II]:*[Type a.b.X.II]);ii(*[Type a.b.X.II]:*[Type a.b.X.II]);\
                set(*[Type a.b.X.II]:0[Type java.util.Set<a.b.X.II>])]\
                """, add2Mtl.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("e(0:*);ii(0:*);j(0:*);set(0:0)", tlvT1.toString());
        assertEquals("""
                e(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                ii(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                j(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                set(0[Type java.util.List<a.b.X.II>]:0[Type java.util.Set<a.b.X.II>])\
                """, tlvT1.toString());
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
        tlc.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("e(0:*);j(0:*);set(0:0)", tlvT1.toString());
        assertEquals("""
                e(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                j(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                set(0[Type java.util.List<a.b.X.II>]:0[Type java.util.Set<a.b.X.II>])\
                """, tlvT1.toString());
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
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("e(0:*);ii(0:*);j(0:*);set(0:0)", tlvT1.toString());
        assertEquals("""
                e(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                ii(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                j(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                set(0[Type java.util.List<a.b.X.II>]:0[Type java.util.Set<a.b.X.II>])\
                """, tlvT1.toString());
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
                void method(List<II> list, X x) {
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
        MethodInfo add = X.findUniqueMethod("add", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("", mtlAdd.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("ii(0:*);j(0:*)", tlvT1.toString());
        assertEquals("""
                ii(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                j(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II])\
                """, tlvT1.toString());
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
        assertEquals("""
                [#1:map(*[Type a.b.X.II]:1[Type java.util.Map<Integer,a.b.X.II>]);v(*[Type a.b.X.II]:*[Type a.b.X.II])]\
                """, mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("put2", 2);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                [#1:ii(*[Type a.b.X.II]:*[Type a.b.X.II]);\
                map(*[Type a.b.X.II]:1[Type java.util.Map<Integer,a.b.X.II>]);\
                v(*[Type a.b.X.II]:*[Type a.b.X.II])]\
                """, add2Mtl.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("ii(1:*);j(1:*);k(0:*);map(1:1);v(1:*)", tlvT1.toString());
        assertEquals("""
                ii(1[Type java.util.Map<Integer,a.b.X.II>]:*[Type a.b.X.II]);\
                j(1[Type java.util.Map<Integer,a.b.X.II>]:*[Type a.b.X.II]);\
                k(0[Type java.util.Map<Integer,a.b.X.II>]:*[Type Integer]);\
                map(1[Type java.util.Map<Integer,a.b.X.II>]:1[Type java.util.Map<Integer,a.b.X.II>]);\
                v(1[Type java.util.Map<Integer,a.b.X.II>]:*[Type a.b.X.II])\
                """, tlvT1.toString());
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
                 void method(Map<H, II> map) {
                    map.forEach((p0, p1) -> put(p0, p1));
                 }
            }
            """;

    @DisplayName("map.forEach((p0, p1) -> put(p0, p1))")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("put", 2);
        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                [#0:k(*[Type a.b.X.H]:*[Type a.b.X.H]);map(*[Type a.b.X.H]:0[Type java.util.Map<a.b.X.H,a.b.X.II>]), \
                #1:map(*[Type a.b.X.II]:1[Type java.util.Map<a.b.X.H,a.b.X.II>]);v(*[Type a.b.X.II]:*[Type a.b.X.II])]\
                """, mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("put2", 2);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                [#0:h(*[Type a.b.X.H]:*[Type a.b.X.H]);k(*[Type a.b.X.H]:*[Type a.b.X.H]);\
                map(*[Type a.b.X.H]:0[Type java.util.Map<a.b.X.H,a.b.X.II>]), \
                #1:ii(*[Type a.b.X.II]:*[Type a.b.X.II]);\
                map(*[Type a.b.X.II]:1[Type java.util.Map<a.b.X.H,a.b.X.II>]);\
                v(*[Type a.b.X.II]:*[Type a.b.X.II])]\
                """, add2Mtl.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo map = method.parameters().getFirst();
        VariableInfo mapVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(map.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvList = mapVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("h(0:*);ii(1:*);k(0:*);map(0:0,1:1);p0(0:*);p1(1:*);v(1:*)", tlvList.toString());
        assertEquals("""
                h(0[Type java.util.Map<a.b.X.H,a.b.X.II>]:*[Type a.b.X.H]);\
                ii(1[Type java.util.Map<a.b.X.H,a.b.X.II>]:*[Type a.b.X.II]);\
                k(0[Type java.util.Map<a.b.X.H,a.b.X.II>]:*[Type a.b.X.H]);\
                map(0[Type java.util.Map<a.b.X.H,a.b.X.II>]:0[Type java.util.Map<a.b.X.H,a.b.X.II>],\
                1[Type java.util.Map<a.b.X.H,a.b.X.II>]:1[Type java.util.Map<a.b.X.H,a.b.X.II>]);\
                p0(0[Type java.util.Map<a.b.X.H,a.b.X.II>]:*[Type a.b.X.H]);\
                p1(1[Type java.util.Map<a.b.X.H,a.b.X.II>]:*[Type a.b.X.II]);\
                v(1[Type java.util.Map<a.b.X.H,a.b.X.II>]:*[Type a.b.X.II])\
                """, tlvList.toString());
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
        tlc.doPrimaryType(X);

        MethodInfo add = X.findUniqueMethod("put", 2);
        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                [#0:k(*[Type a.b.X.II]:*[Type a.b.X.II]);map(*[Type a.b.X.II]:0[Type java.util.Map<a.b.X.II,a.b.X.H>]), \
                #1:map(*[Type a.b.X.H]:1[Type java.util.Map<a.b.X.II,a.b.X.H>]);v(*[Type a.b.X.H]:*[Type a.b.X.H])]\
                """, mtlAdd.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo map = method.parameters().getFirst();
        VariableInfo mapVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(map.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvList = mapVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("h(0:*);ii(1:*);k(1:*);map(0:1,1:0);p0(0:*);p1(1:*);v(0:*)", tlvList.toString());
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
        TypeInfo X = javaInspector.parse(INPUT10);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);
    }

}
