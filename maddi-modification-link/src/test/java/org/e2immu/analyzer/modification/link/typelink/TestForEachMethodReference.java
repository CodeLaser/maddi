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
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
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
        Links tlvSet0 = set0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("ii(*[Type a.b.X.II]:0[Type java.util.Set<a.b.X.II>])", tlvSet0.toString());
        MethodLinkedVariables mtlNext = add.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[#0:e(*[Type a.b.X.II]:*[Type a.b.X.II]);set(*[Type a.b.X.II]:0[Type java.util.Set<a.b.X.II>])]",
                mtlNext.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);

        // NOTE: any parameter "e" that shows up: java.util.Set.add(E):0:e
        assertEquals("e(0:*);ii(0:*);set(0:0)", tlvT1.toString());
        assertEquals("""
                e(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                ii(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
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
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("set(0:0)", tlvT1.toString());
        assertEquals("""
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
        MethodInfo add = X.findUniqueMethod("add", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 2);

        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("e(0:*);ii(0:*);set(0:0)", tlvT1.toString());
        assertEquals("""
                e(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
                ii(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II]);\
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
        assertEquals("", mtlAdd.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        Statement forEach = method.methodBody().statements().getFirst();
        ParameterInfo list = method.parameters().getFirst();
        VariableInfo listVi = VariableDataImpl.of(forEach).variableInfoContainerOrNull(list.fullyQualifiedName())
                .best(Stage.EVALUATION);
        Links tlvT1 = listVi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("ii(0:*)", tlvT1.toString());
        assertEquals("ii(0[Type java.util.List<a.b.X.II>]:*[Type a.b.X.II])", tlvT1.toString());
    }

}
