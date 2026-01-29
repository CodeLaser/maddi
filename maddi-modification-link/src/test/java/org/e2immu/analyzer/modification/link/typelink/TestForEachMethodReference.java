package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.This;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class TestForEachMethodReference extends CommonTest {

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class X {
                 interface I { }
                 class II implements I { void implementationMethod() { } }
                 Set<II> set = new HashSet<>();
                 void add(II ii) {
                     set.add(ii);
                     ii.implementationMethod();
                 }
                 void method(List<II> list) {
                    list.forEach(this::add);
                 }
            }
            """;

    @DisplayName("list.forEach(this::add)")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("add", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        VariableData vdAdd0 = VariableDataImpl.of(add.methodBody().statements().getFirst());
        VariableInfo set0 = vdAdd0.variableInfoContainerOrNull("a.b.X.set").best(Stage.EVALUATION);
        Links tlvSet0 = set0.linkedVariablesOrEmpty();
        assertEquals("this.set.§$s∋0:ii", tlvSet0.toString());
        MethodLinkedVariables mlvAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:ii∈this.set*.§$s] --> -", mlvAdd.toString());
        assertEquals("this, this.set", mlvAdd.sortedModifiedString());

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~this.set.§$s", tlvT1.toString());

        // note that "this" in "this::add" points to a.b.X, and not the this of the lambda anonymous type
        MethodCall mc = (MethodCall) forEach.expression();
        MethodReference mr = (MethodReference) mc.parameterExpressions().getFirst();
        if (mr.scope() instanceof VariableExpression ve && ve.variable() instanceof This thisVar) {
            assertEquals("a.b.X", thisVar.typeInfo().fullyQualifiedName());
        } else fail();

        MethodLinkedVariablesImpl mlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("this, this.set", mlvMethod.sortedModifiedString());
        assertEquals("[0:list.§$s~this.set*.§$s] --> -", mlvMethod.toString());
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
                    list.forEach(set::add);
                 }
            }
            """;

    @DisplayName("list.forEach(this.set::add)")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 1);
        tlc.doPrimaryType(X);
        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~this.set.§es", tlvT1.toString());
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
                    list.forEach(x::add);
                 }
            }
            """;

    @DisplayName("list.forEach(x::add)")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("add", 1);
        MethodLinkedVariablesImpl mlvAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:ii∈this.set*.§$s] --> -", mlvAdd.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("0:list.§$s~1:x.set.§$s", tlvT1.toString());
        MethodLinkedVariables mlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:list.§$s~1:x.set*.§$s, 1:x.set*.§$s~0:list.§$s] --> -", mlv.toString());
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
                    list.forEach(X::add);
                }
            }
            """;

    @DisplayName("list.forEach(X::add)")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo add = X.findUniqueMethod("add", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodLinkedVariables mtlAdd = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> -", mtlAdd.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.linkedVariablesOrEmpty();
        assertEquals("-", tlvT1.toString());

        assertEquals("[-, -] --> -", method.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class).toString());
    }

}
