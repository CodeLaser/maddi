package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSupplier extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Optional;
            import java.util.function.Supplier;
            public class C<X> {
                public X method(Optional<X> optional, X alternative) {
                    X x = optional.orElseGet(() -> alternative);
                    return x;
                }
                public X method2(Optional<X> optional, X alternative) {
                    Supplier<X> lambda = () -> alternative;
                    X x = optional.orElseGet(lambda);
                    return x;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = C.findUniqueMethod("method", 2);

        TypeInfo optional = javaInspector.compiledTypesManager().get(Optional.class);
        MethodInfo orElseGet = optional.findUniqueMethod("orElseGet", 1);
        MethodLinkedVariables mlvOrElseGet = orElseGet.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(orElseGet));
        assertEquals("""
                [-] --> orElseGet←this.§t,orElseGet←Λ0:supplier\
                """, mlvOrElseGet.toString());

        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.linkedVariablesOrEmpty();
        assertEquals("x←1:alternative,x←0:optional.§x", tlvX.toString());

        assertEquals("[-, -] --> method←1:alternative,method←0:optional.§x", mlvMethod.toString());
    }

    @Test
    public void test1b() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method2 = C.findUniqueMethod("method2", 2);
        MethodLinkedVariables mlvMethod = method2.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(method2));

        VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
        VariableInfo viLambda0 = vd0.variableInfo("lambda");
        assertEquals("lambda←Λ$_fi0", viLambda0.linkedVariables().toString());
        FunctionalInterfaceVariable fi0 = (FunctionalInterfaceVariable) viLambda0.linkedVariables()
                .stream().findFirst().orElseThrow().to();
        assertEquals("Result{links=get←1:alternative, evaluated=lambda}", fi0.result().toString());
        assertEquals("[-, -] --> method2←1:alternative,method2←0:optional.§x", mlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Optional;
            public class C<X> {
                X alternative;
                private X supplier() {
                    return alternative;
                }
                public X method(Optional<X> optional) {
                    X x = optional.orElseGet(this::supplier);
                    return x;
                }
            }
            """;


    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo supplier = C.findUniqueMethod("supplier", 0);
        MethodLinkedVariables mlvSupplier = supplier.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(supplier));
        assertEquals("[] --> supplier←this.alternative", mlvSupplier.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.linkedVariablesOrEmpty();
        assertEquals("x←this.alternative,x←0:optional.§x", tlvX.toString());

        assertEquals("[-] --> method←this.alternative,method←0:optional.§x", mlv.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Optional;
            public class C<X> {
                X alternative;
                private X supplier() {
                    return alternative;
                }
                public X method(Optional<X> optional, C<X> c) {
                    X x = optional.orElseGet(c::supplier);
                    return x;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 2);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.linkedVariablesOrEmpty();
        assertEquals("x←1:c.alternative,x←0:optional.§x", tlvX.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Optional;
            public class C<X> {
                X alternative;
                private X supplier() {
                    return alternative;
                }
                public static <X> X callSupplier(C<X> c) {
                    return c.supplier();
                }
                public X method(Optional<X> optional, C<X> c) {
                    X x = optional.orElseGet(() -> c.supplier());
                    return x;
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo supplier = C.findUniqueMethod("supplier", 0);
        MethodLinkedVariables mlvSupplier = supplier.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[] --> supplier←this.alternative", mlvSupplier.toString());

        MethodInfo callSupplier = C.findUniqueMethod("callSupplier", 1);
        MethodLinkedVariables mlvCallSupplier = callSupplier.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> callSupplier←0:c.alternative", mlvCallSupplier.toString());

        MethodInfo method = C.findUniqueMethod("method", 2);
        MethodLinkedVariables mlvMethod = method.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.linkedVariablesOrEmpty();
        assertEquals("x←1:c.alternative,x←0:optional.§x", tlvX.toString());

        assertEquals("[0:optional≈1:c, -] --> method←1:c.alternative,method←0:optional.§x", mlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.Optional;
            import java.util.List;
            import java.util.function.Supplier;
            public class C<X> {
                public List<X> method(Optional<List<X>> optional, List<X> main) {
                    List<X> xList = optional.orElseGet(() -> main.subList(0, 2));
                    return xList;
                }
                public List<X> method2(Optional<List<X>> optional, List<X> main) {
                    Supplier<List<X>> lambda = () -> main.subList(0, 2);
                    List<X> xList = optional.orElseGet(lambda);
                    return xList;
                }
            }
            """;

    @Test
    public void test5Method() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo optional = javaInspector.compiledTypesManager().get(Optional.class);
        MethodInfo orElseGet = optional.findUniqueMethod("orElseGet", 1);
        MethodLinkedVariables mlvOrElseGet = orElseGet.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(orElseGet));
        assertEquals("[-] --> orElseGet←this.§t,orElseGet←Λ0:supplier", mlvOrElseGet.toString());

        MethodInfo method = C.findUniqueMethod("method", 2);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("xList");
        Links tlvX = viX0.linkedVariablesOrEmpty();
        assertEquals("""
                xList.§m←1:main.§m,xList.§m←0:optional.§m,xList.§xs←1:main.§xs,xList.§xs⊆0:optional.§xs\
                """, tlvX.toString());

        assertEquals("""
                [0:optional.§xs⊇1:main.§xs, 1:main.§xs⊆0:optional.§xs] --> \
                method.§m←1:main.§m,method.§m←0:optional.§m,method.§xs←1:main.§xs,method.§xs⊆0:optional.§xs\
                """, mlvMethod.toString());
    }

    @Test
    public void test5Method2() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = C.findUniqueMethod("method2", 2);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viLambda0 = vd0.variableInfo("lambda");
        assertEquals("lambda←Λ$_fi3", viLambda0.linkedVariables().toString());

        assertEquals("""
                [0:optional.§xs⊇1:main.§xs, 1:main.§xs⊆0:optional.§xs] --> \
                method2.§m←1:main.§m,method2.§m←0:optional.§m,method2.§xs←1:main.§xs,method2.§xs⊆0:optional.§xs\
                """, mlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.Optional;
            import java.util.function.Supplier;
            public class C<X, Y> {
                public Map.Entry<X, Y> method(Optional<Map.Entry<X, Y>> optional, X altX, Y altY) {
                    Map.Entry<X, Y> entry = optional.orElseGet(() -> new AbstractMap.SimpleEntry<>(altX, altY));
                    return entry;
                }
                public Map.Entry<X, Y> method2(Optional<Map.Entry<X, Y>> optional, X altX, Y altY) {
                    Supplier<Map.Entry<X, Y>> lambda = () -> new AbstractMap.SimpleEntry<>(altX, altY);
                    Map.Entry<X, Y> entry = optional.orElseGet(lambda);
                    return entry;
                }
            }
            """;

    @Test
    public void test7() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT7);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        {
            MethodInfo method2 = C.findUniqueMethod("method2", 3);
            MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));

            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
            VariableInfo viLambda = vd0.variableInfo("lambda");
            Links lvLambda = viLambda.linkedVariablesOrEmpty();
            assertEquals("lambda←Λ$_fi1", lvLambda.toString());

            VariableData vd1 = VariableDataImpl.of(method2.methodBody().statements().get(1));
            VariableInfo viEntry = vd1.variableInfo("entry");
            Links lvEntry = viEntry.linkedVariablesOrEmpty();
            assertEquals("""
                    entry←0:optional.§xy,entry.§xy.§x≺0:optional,entry.§xy.§x←1:altX,entry.§xy.§x≺0:optional.§xy,entry.§xy.§y≺0:optional,entry.§xy.§y←2:altY,entry.§xy.§y≺0:optional.§xy,entry.§xy≺0:optional,entry.§xy≺0:optional.§xy\
                    """, lvEntry.toString());

            assertEquals("""
                    [-, -, -] --> method2←0:optional.§xy,method2.§xy.§x←1:altX,method2.§xy.§y←2:altY\
                    """, mlv2.toString());
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 3);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            assertEquals("""
                    [-, -, -] --> method←0:optional.§xy,method.§xy.§x←1:altX,method.§xy.§y←2:altY\
                    """, mlv.toString());
        }
    }
}
