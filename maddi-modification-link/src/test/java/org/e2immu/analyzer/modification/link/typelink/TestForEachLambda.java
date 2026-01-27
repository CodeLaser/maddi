package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("[0:ii∈this.set*.§$s] --> -", mtlAdd.toString());
        assertEquals("this, this.set", mtlAdd.sortedModifiedString());

        // propagation of modifications of the parameter
        MethodInfo add2 = X.findUniqueMethod("add2", 1);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(add2));
        assertEquals("[0:ii*∈this.set*.§$s] --> -", add2Mtl.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        // modification must survive a lambda
        Statement forEach = method.methodBody().statements().getFirst();
        Lambda lambda = (Lambda) ((MethodCall) forEach.expression()).parameterExpressions().getFirst();
        MethodInfo lambdaMethod = lambda.methodInfo();
        Statement lambdaAdd = lambda.methodBody().statements().getFirst();
        VariableData vdLambda = VariableDataImpl.of(lambdaAdd);
        // so while we refer to this.set.§$s, we don't have it in the known variables
        assertEquals("a.b.X.$0.accept(a.b.X.II):0:j, a.b.X.this", vdLambda.knownVariableNamesToString());
        VariableInfo viJ = vdLambda.variableInfo(lambdaMethod.parameters().getFirst());
        assertEquals("0:j∈this.set.§$s", viJ.linkedVariables().toString());

        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~this.set.§$s", tlvT1.toString());

        assertEquals("[0:list.§$s~this.set*.§$s] --> -", mlv.toString());
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

        Lambda lambda = (Lambda) ((MethodCall) forEach.expression()).parameterExpressions().getFirst();
        MethodInfo lambdaMethod = lambda.methodInfo();
        Statement lambdaAdd = lambda.methodBody().statements().getFirst();
        VariableData vdLambda = VariableDataImpl.of(lambdaAdd);
        assertEquals("""
                a.b.X.$0.accept(a.b.X.II), a.b.X.$0.accept(a.b.X.II):0:j, a.b.X.set, a.b.X.this\
                """, vdLambda.knownVariableNamesToString());
        VariableInfo viJ = vdLambda.variableInfo(lambdaMethod.parameters().getFirst());
        assertEquals("0:j∈this.set.§$s", viJ.linkedVariables().toString());
        VariableInfo viSet = vdLambda.variableInfo("a.b.X.set");
        assertTrue(viSet.isModified()); // this now needs to travel to the enclosing method

        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~this.set.§$s", tlvT1.toString());
        assertEquals("[0:list.§$s~this.set*.§$s] --> -", mlv.toString());
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
        MethodLinkedVariablesImpl mlvAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:ii∈this.set*.§$s] --> -", mlvAdd.toString());
        assertTrue(add.isModifying());

        MethodInfo method = X.findUniqueMethod("method", 2);

        Statement forEach = method.methodBody().statements().getFirst();

        Lambda lambda = (Lambda) ((MethodCall) forEach.expression()).parameterExpressions().getFirst();
        MethodInfo lambdaMethod = lambda.methodInfo();
        Statement lambdaAdd = lambda.methodBody().statements().getFirst();
        VariableData vdLambda = VariableDataImpl.of(lambdaAdd);
        assertEquals("""
                a.b.X.$0.accept(a.b.X.II):0:j, a.b.X.method(java.util.List<a.b.X.II>,a.b.X):1:x\
                """, vdLambda.knownVariableNamesToString());
        VariableInfo viJ = vdLambda.variableInfo(lambdaMethod.parameters().getFirst());
        assertEquals("0:j∈1:x.set.§$s", viJ.linkedVariables().toString());
        VariableInfo viX = vdLambda.variableInfo(method.parameters().getLast());
        assertTrue(viX.isModified()); // this now needs to travel to the enclosing method

        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~1:x.set.§$s", tlvT1.toString());
        assertEquals("""
                [0:list.§$s~1:x.set*.§$s, 1:x.set*.§$s~0:list.§$s] --> -\
                """, method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());
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
        assertEquals("[-] --> -", mlvMethod2.toString());

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
        assertEquals("-", tlvT1.toString());

        assertEquals("[-] --> -", mlvMethod.toString());
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
        MethodInfo put = X.findUniqueMethod("put", 2);

        MethodLinkedVariables mlvPut = put.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(put));
        assertEquals("[0:i∈this.map*.§$$s[-1], 1:ii∈this.map*.§$$s[-2]] --> -", mlvPut.toString());

        MethodInfo put2 = X.findUniqueMethod("put2", 2);
        MethodLinkedVariables mlvPut2 = put2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(put2));
        assertEquals("[0:i*∈this.map*.§$$s[-1], 1:ii*∈this.map*.§$$s[-2]] --> -", mlvPut2.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo map = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(map.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();

        Link link = tlvT1.stream().findFirst().orElseThrow();
        assertEquals("map.§$$s[-1]", link.from().toString());
        assertEquals("Type a.b.X.II[]", link.from().parameterizedType().toString());

        assertEquals("""
                0:map.§$$s[-1]~this.map.§$$s[-2],\
                0:map.§$$s[-2]~this.map.§$$s[-1],\
                0:map.§$$s~this.map.§$$s\
                """, tlvT1.toString());
        assertEquals("""
                [0:map.§$$s[-1]~this.map*.§$$s[-2],0:map.§$$s[-2]~this.map*.§$$s[-1],0:map.§$$s~this.map*.§$$s] --> -\
                """, mlv.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        assertEquals("""
                [0:map.§$$s[-1]~this.map*.§$$s[-1],\
                0:map.§$$s[-2]~this.map*.§$$s[-2],\
                0:map.§$$s~this.map*.§$$s] --> -\
                """, mlv2.toString());
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

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("put", 2);
        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:h∈this.map*.§$$s[-1], 1:ii∈this.map*.§$$s[-2]] --> -", mtlAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("put2", 2);
        MethodLinkedVariables add2Mtl = add2.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("[0:h*∈this.map*.§$$s[-1], 1:ii*∈this.map*.§$$s[-2]] --> -", add2Mtl.toString());

        final String LINKS_MAP_PUT = """
                [0:map.§$$s[-1]~this.map*.§$$s[-2],\
                0:map.§$$s[-2]~this.map*.§$$s[-1],\
                0:map.§$$s~this.map*.§$$s] --> -\
                """;

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
        assertEquals("[0:ii∈this.map*.§$$s[-1], 1:h∈this.map*.§$$s[-2]] --> -", mlvPut.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("""
                [0:map.§$$s[-1]~this.map*.§$$s[-1],\
                0:map.§$$s[-2]~this.map*.§$$s[-2],\
                0:map.§$$s~this.map*.§$$s] --> -\
                """, mlvMethod.toString());
    }


    @Language("java")
    private static final String INPUT9b = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.BiConsumer;
            public class X {
                 interface H { }
                 interface I { }
                 class II implements I { }
                 Map<II, H> map = new HashMap<>();
                 void put(II ii, H h) {
                     map.put(ii, h);
                 }
                 void method(Map<H, II> map) {
                    map.forEach(new BiConsumer<a.b.X.H, a.b.X.II>() {
                        @Override 
                        public void accept(a.b.X.H h, a.b.X.II ii) {
                            put(ii, h);
                        }
                    });
                 }
            }
            """;


    @DisplayName("map.forEach((p0, p1) -> put(p1, p0)) using anonymous class construction")
    @Test
    public void test9b() {
        TypeInfo X = javaInspector.parse(INPUT9b);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo put = X.findUniqueMethod("put", 2);
        MethodLinkedVariables mlvPut = put.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(put));
        assertEquals("[0:ii∈this.map*.§$$s[-1], 1:h∈this.map*.§$$s[-2]] --> -", mlvPut.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("""
                [0:map.§$$s[-1]~this.map*.§$$s[-1],\
                0:map.§$$s[-2]~this.map*.§$$s[-2],\
                0:map.§$$s~this.map*.§$$s] --> -\
                """, mlvMethod.toString());
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
