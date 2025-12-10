package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
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
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 2);

        TypeInfo optional = javaInspector.compiledTypesManager().get(Optional.class);
        MethodInfo orElseGet = optional.findUniqueMethod("orElseGet", 1);
        MethodLinkedVariables tlvGetKey = orElseGet.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                return orElseGet(*[Type param T]:0[Type java.util.Optional<T>])\
                [#0:return orElseGet(0[Type java.util.function.Supplier<? extends T>]:0[Type java.util.Optional<T>])]\
                """, tlvGetKey.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                alternative(*[Type param X]:*[Type param X]);\
                optional(*[Type param X]:0[Type java.util.Optional<X>])\
                """, tlvX.toString());
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
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 1);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                alternative(*[Type param X]:*[Type param X]);\
                optional(*[Type param X]:0[Type java.util.Optional<X>])\
                """, tlvX.toString());
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
        assertEquals("""
                alternative(*[Type param X]:*[Type param X]);\
                optional(*[Type param X]:0[Type java.util.Optional<X>])\
                """, tlvX.toString());
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

        MethodInfo callSupplier = C.findUniqueMethod("callSupplier", 1);
        MethodLinkedVariables tlvCallSupplier = callSupplier.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                alternative(0[Type a.b.C<X>]:*[Type param X])\
                """, tlvCallSupplier.toString());

        MethodInfo method = C.findUniqueMethod("method", 2);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("x");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        // we care more about correctness of the RHS than of that of the LHS
        // but the LHS should not be "wrong". 0 in C<X> is not incorrect, Type param X would have been better
        assertEquals("""
                alternative(0[Type a.b.C<X>]:*[Type param X]);\
                optional(*[Type param X]:0[Type java.util.Optional<X>])\
                """, tlvX.toString());
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
            }
            """;

    @Test
    public void test5() {
        TypeInfo C = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo method = C.findUniqueMethod("method", 3);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("entry");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);

        // we care more about correctness of the RHS than of that of the LHS
        // but the LHS should not be "wrong".
        // 0 in SimpleEntry<K,V> is not wrong but 0 in Map.Entry<X,Y> would have been better
        assertEquals("""
                altX(0[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param X]);\
                altY(1[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param Y]);\
                optional(*[Type java.util.Map.Entry<X,Y>]:0[Type java.util.Optional<java.util.Map.Entry<X,Y>>])\
                """, tlvX.toString());
    }
}
