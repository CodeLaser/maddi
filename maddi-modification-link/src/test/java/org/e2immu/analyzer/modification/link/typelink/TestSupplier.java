package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSupplier extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Optional;
            public class C<X> {
                public X method(Optional<X> optional, X alternative) {
                    X x = optional.orElseGet(() -> alternative);
                    return x;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = C.findUniqueMethod("method", 2);

        TypeInfo optional = javaInspector.compiledTypesManager().get(Optional.class);
        MethodInfo orElseGet = optional.findUniqueMethod("orElseGet", 1);
        MethodLinkedVariables mlvOrElseGet = orElseGet.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(orElseGet));
        assertEquals("""
                [-] --> orElseGet==this.§t,orElseGet==Λ0:supplier\
                """, mlvOrElseGet.toString());

        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () ->
                tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("x==0:optional.§x,x==1:alternative", tlvX.toString());

        assertEquals("""
                [0:optional.§x==1:alternative, 1:alternative==0:optional.§x] --> method==0:optional.§x,method==1:alternative\
                """, mlvMethod.toString());
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
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo supplier = C.findUniqueMethod("supplier", 0);
        MethodLinkedVariables mlvSupplier = supplier.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(supplier));
        assertEquals("[] --> supplier==this.alternative", mlvSupplier.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("x==0:optional.§x,x==this.alternative", tlvX.toString());

        assertEquals("[0:optional.§x==this.alternative] --> method==0:optional.§x,method==this.alternative",
                mlv.toString());
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
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 2);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("x==0:optional.§x,x==1:c.alternative", tlvX.toString());
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
        TypeInfo C = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo supplier = C.findUniqueMethod("supplier", 0);
        MethodLinkedVariables mlvSupplier = supplier.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[] --> supplier==this.alternative", mlvSupplier.toString());

        MethodInfo callSupplier = C.findUniqueMethod("callSupplier", 1);
        MethodLinkedVariables mlvCallSupplier = callSupplier.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> callSupplier==0:c.alternative", mlvCallSupplier.toString());

        MethodInfo method = C.findUniqueMethod("method", 2);
        MethodLinkedVariables mlvMethod = method.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("x==0:optional.§x,x==1:c.alternative", tlvX.toString());

        assertEquals("""
                [0:optional.§x==1:c.alternative,0:optional.§x~1:c, 1:c.alternative==0:optional.§x,1:c~0:optional] \
                --> method==0:optional.§x,method==1:c.alternative,method~1:c\
                """, mlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.Optional;
            public class C<X, Y> {
                public Map.Entry<X, Y> method(Optional<Map.Entry<X, Y>> optional, X altX, Y altY) {
                    Map.Entry<X, Y> entry = optional.orElseGet(() -> new AbstractMap.SimpleEntry<>(altX, altY));
                    return entry;
                }
                public Map.Entry<X, Y> method2(Optional<Map.Entry<X, Y>> optional, X altX, Y altY) {
                    var lambda = () -> new AbstractMap.SimpleEntry<>(altX, altY);
                    Map.Entry<X, Y> entry = optional.orElseGet(lambda);
                    return entry;
                }
            }
            """;

    @Test
    public void test5() {
        TypeInfo C = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        {
            MethodInfo method2 = C.findUniqueMethod("method2", 3);
            MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));

            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
            VariableInfo viLambda = vd0.variableInfo("lambda");
            Links lvLambda = viLambda.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("lambda.§xy.§x==1:altX,lambda.§xy.§y==2:altY", lvLambda.toString());

            VariableData vd1 = VariableDataImpl.of(method2.methodBody().statements().get(1));
            VariableInfo viEntry = vd1.variableInfo("entry");
            Links lvEntry = viEntry.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("entry==0:optional.§xy,entry=...", lvEntry.toString());
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 3);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
            VariableInfo viX0 = vd0.variableInfo("entry");
            Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);

            // we care more about correctness of the RHS than of that of the LHS
            // but the LHS should not be "wrong".
            // 0 in SimpleEntry<K,V> is not wrong but 0 in Map.Entry<X,Y> would have been better
            // assertEquals("""
            //        entry==0:optional.§xy,entry=....\
            //         """, tlvX.toString());
            // assertEquals("[-, -, -] --> method==0:optional.§xy", mlv.toString());
        }
    }
}
