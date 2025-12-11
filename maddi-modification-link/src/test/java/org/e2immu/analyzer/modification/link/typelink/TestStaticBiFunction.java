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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestStaticBiFunction extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private Y iy;
                public static <X, Y> X extract(X x, Y y) {
                    return x;
                }
                X make(BiFunction<X, Y, X> biFunction) {
                    return biFunction.apply(ix, iy);
                }
                X method() {
                     X xx = make(C::extract);
                     return xx;
                }
            }
            """;

    @DisplayName("BiFunction extract")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 0);

        MethodInfo join = C.findUniqueMethod("extract", 2);
        MethodLinkedVariables tlvJoin = join.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                x(*[Type param X]:*[Type param X])\
                """, tlvJoin.toString());

        MethodInfo make = C.findUniqueMethod("make", 1);
        MethodLinkedVariables tlvMake = make.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                biFunction(*[Type param X]:2[Type java.util.function.BiFunction<X,Y,X>])\
                [#0:ix(*[Type param X]:0[Type java.util.function.BiFunction<X,Y,X>]);\
                iy(*[Type param Y]:1[Type java.util.function.BiFunction<X,Y,X>])]\
                """, tlvMake.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viEntry0 = vd0.variableInfo("xx");
        Links tlvEntry = viEntry0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                ix(*[Type param X]:*[Type param X]);x(*[Type param X]:0[Type java.util.function.BiFunction<X,Y,X>])\
                """, tlvEntry.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            import java.util.Set;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private Y iy;
                public static <X, Y> List<Set<X>> extract(X x, Y y) {
                    return List.of(Set.of(x));
                }
                List<Set<X>> make(BiFunction<X, Y, List<Set<X>>> biFunction) {
                    return biFunction.apply(ix, iy);
                }
                List<Set<X>> method() {
                     List<Set<X>> xx = make(C::extract);
                     return xx;
                }
            }
            """;

    @DisplayName("BiFunction extract wrapped")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo method = C.findUniqueMethod("method", 0);

        MethodInfo join = C.findUniqueMethod("extract", 2);
        MethodLinkedVariables tlvJoin = join.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        // NOTE the E instead of X... on the LHS, we can have formal values
        assertEquals("""
                x([0]0[Type java.util.List<java.util.Set<E>>]:*[Type param X])\
                """, tlvJoin.toString());

        MethodInfo make = C.findUniqueMethod("make", 1);
        MethodLinkedVariables tlvMake = make.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                biFunction(*[Type java.util.List<java.util.Set<X>>]:2[Type java.util.function.BiFunction<X,Y,java.util.List<java.util.Set<X>>>])\
                [#0:ix(*[Type param X]:0[Type java.util.function.BiFunction<X,Y,java.util.List<java.util.Set<X>>>]);\
                iy(*[Type param Y]:1[Type java.util.function.BiFunction<X,Y,java.util.List<java.util.Set<X>>>])]\
                """, tlvMake.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viEntry0 = vd0.variableInfo("xx");
        Links tlvEntry = viEntry0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                ix(*[Type param X]:*[Type param X]);\
                x([0]0[Type java.util.List<java.util.Set<X>>]:0[Type java.util.function.BiFunction<X,Y,java.util.List<java.util.Set<X>>>])\
                """, tlvEntry.toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private Y iy;
                public static <X, Y> Map.Entry<X, Y> join(X x, Y y) {
                    return new AbstractMap.SimpleEntry<>(x, y);
                }
                Map.Entry<X, Y> make(BiFunction<X, Y, Map.Entry<X, Y>> biFunction) {
                    return biFunction.apply(ix, iy);
                }
                Map.Entry<X, Y> method() {
                     Map.Entry<X, Y> entry = make(C::join);
                     return entry;
                }
            }
            """;

    @DisplayName("BiFunction join")
    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 0);

        MethodInfo join = C.findUniqueMethod("join", 2);
        MethodLinkedVariables tlvJoin = join.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                x(0[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param X]);\
                y(1[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param Y])\
                """, tlvJoin.toString());

        MethodInfo make = C.findUniqueMethod("make", 1);
        MethodLinkedVariables tlvMake = make.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                biFunction(*[Type java.util.Map.Entry<X,Y>]:2[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<X,Y>>])\
                [#0:ix(*[Type param X]:0[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<X,Y>>]);\
                iy(*[Type param Y]:1[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<X,Y>>])]\
                """, tlvMake.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viEntry0 = vd0.variableInfo("entry");
        Links tlvEntry = viEntry0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                ix(*[Type param X]:*[Type param X]);\
                iy(*[Type param Y]:*[Type param Y]);\
                x(0[Type java.util.Map.Entry<X,Y>]:0[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<X,Y>>]);\
                y(1[Type java.util.Map.Entry<X,Y>]:1[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<X,Y>>])\
                """, tlvEntry.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private Y iy;
                public static <X, Y> Map.Entry<Y, X> join(X x, Y y) {
                    return new AbstractMap.SimpleEntry<>(y, x);
                }
                Map.Entry<Y, X> make(BiFunction<X, Y, Map.Entry<Y, X>> biFunction) {
                    return biFunction.apply(ix, iy);
                }
                Map.Entry<Y, X> method() {
                     Map.Entry<Y, X> entry = make(C::join);
                     return entry;
                }
            }
            """;

    @DisplayName("BiFunction join reversed")
    @Test
    public void test4() {
        TypeInfo C = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 0);

        MethodInfo join = C.findUniqueMethod("join", 2);
        MethodLinkedVariables tlvJoin = join.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                x(1[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param X]);\
                y(0[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param Y])\
                """, tlvJoin.toString());

        MethodInfo make = C.findUniqueMethod("make", 1);
        MethodLinkedVariables tlvMake = make.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                biFunction(*[Type java.util.Map.Entry<Y,X>]:2[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<Y,X>>])\
                [#0:ix(*[Type param X]:0[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<Y,X>>]);\
                iy(*[Type param Y]:1[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<Y,X>>])]\
                """, tlvMake.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viEntry0 = vd0.variableInfo("entry");
        Links tlvEntry = viEntry0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                ix(*[Type param X]:*[Type param X]);\
                iy(*[Type param Y]:*[Type param Y]);\
                x(1[Type java.util.Map.Entry<Y,X>]:0[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<Y,X>>]);\
                y(0[Type java.util.Map.Entry<Y,X>]:1[Type java.util.function.BiFunction<X,Y,java.util.Map.Entry<Y,X>>])\
                """, tlvEntry.toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.List;
            import java.util.Map;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private List<Y> iys;
                public static <X, Y> Map.Entry<Y, X> join(X x, List<Y> ys) {
                    return new AbstractMap.SimpleEntry<>(ys.getFirst(), x);
                }
                Map.Entry<Y, X> make(BiFunction<X, List<Y>, Map.Entry<Y, X>> biFunction) {
                    return biFunction.apply(ix, iys);
                }
                Map.Entry<Y, X> method() {
                     Map.Entry<Y, X> entry = make(C::join);
                     return entry;
                }
            }
            """;

    @DisplayName("BiFunction join reversed wrapped")
    @Test
    public void test5() {
        TypeInfo C = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 0);

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        MethodInfo getFirst = list.findUniqueMethod("getFirst", 0);
        MethodLinkedVariables tlvGetFirst = getFirst.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                return getFirst(*[Type param E]:0[Type java.util.List<E>])\
                """, tlvGetFirst.toString());

        MethodInfo join = C.findUniqueMethod("join", 2);
        MethodLinkedVariables tlvJoin = join.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                x(1[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param X]);\
                ys(0[Type java.util.AbstractMap.SimpleEntry<K,V>]:0[Type java.util.List<Y>])\
                """, tlvJoin.toString());

        MethodInfo make = C.findUniqueMethod("make", 1);
        MethodLinkedVariables tlvMake = make.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                biFunction(*[Type java.util.Map.Entry<Y,X>]:2[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])\
                [#0:ix(*[Type param X]:0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>]);\
                iys(*[Type java.util.List<Y>]:1[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])]\
                """, tlvMake.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viEntry0 = vd0.variableInfo("entry");
        Links tlvEntry = viEntry0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                ix(*[Type param X]:*[Type param X]);\
                iys(*[Type java.util.List<Y>]:*[Type java.util.List<Y>]);\
                x(1[Type java.util.Map.Entry<Y,X>]:0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>]);\
                ys(0[Type java.util.Map.Entry<Y,X>]:[1]0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])\
                """, tlvEntry.toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.List;
            import java.util.Map;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private Y iy;
                public static <X, Y> Map.Entry<Y, X> join(X x, List<Y> ys) {
                    return new AbstractMap.SimpleEntry<>(ys.getFirst(), x);
                }
                Map.Entry<Y, X> make(BiFunction<X, List<Y>, Map.Entry<Y, X>> biFunction) {
                    return biFunction.apply(ix, List.of(iy));
                }
                Map.Entry<Y, X> method() {
                     Map.Entry<Y, X> entry = make(C::join);
                     return entry;
                }
            }
            """;

    @DisplayName("BiFunction join reversed wrapped 2")
    @Test
    public void test6() {
        TypeInfo C = javaInspector.parse(INPUT6);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 0);

        MethodInfo join = C.findUniqueMethod("join", 2);
        MethodLinkedVariables tlvJoin = join.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                x(1[Type java.util.AbstractMap.SimpleEntry<K,V>]:*[Type param X]);\
                ys(0[Type java.util.AbstractMap.SimpleEntry<K,V>]:0[Type java.util.List<Y>])\
                """, tlvJoin.toString());

        MethodInfo make = C.findUniqueMethod("make", 1);
        MethodLinkedVariables tlvMake = make.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                biFunction(*[Type java.util.Map.Entry<Y,X>]:2[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])\
                [#0:ix(*[Type param X]:0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>]);\
                iy(0[Type java.util.List<Y>]:[1]0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])]\
                """, tlvMake.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viEntry0 = vd0.variableInfo("entry");
        Links tlvEntry = viEntry0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                ix(*[Type param X]:*[Type param X]);\
                iy(0[Type java.util.List<Y>]:0[Type java.util.List<Y>]);\
                x(1[Type java.util.Map.Entry<Y,X>]:0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>]);\
                ys(0[Type java.util.Map.Entry<Y,X>]:[1]0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])\
                """, tlvEntry.toString());
        String s2 = """
                ix(*[Type param X]:*[Type param X]);\
                iy(*[Type param X]:0[Type java.util.List<Y>]);\
                x(1[Type java.util.Map.Entry<Y,X>]:0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>]);\
                ys(0[Type java.util.Map.Entry<Y,X>]:[1]0[Type java.util.function.BiFunction<X,java.util.List<Y>,java.util.Map.Entry<Y,X>>])\
                """;
    }

}
