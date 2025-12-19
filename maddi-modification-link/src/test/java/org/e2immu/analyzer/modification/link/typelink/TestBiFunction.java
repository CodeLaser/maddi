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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestBiFunction extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.function.BiFunction;
            public class C<X, Y> {
                private X ix;
                private X jx;
                private Y iy;
                public X extract(X x, Y y) {
                    return y == null ? x : jx;
                }
                X make(BiFunction<X, Y, X> biFunction) {
                    return biFunction.apply(ix, iy);
                }
                X method() {
                     X xx = make(this::extract);
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
                jx(*[Type param X]:*[Type param X]);\
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
                ix(*[Type param X]:*[Type param X]);\
                jx(0[Type a.b.C<X,Y>]:*[Type param X]);\
                x(*[Type param X]:0[Type java.util.function.BiFunction<X,Y,X>])\
                """, tlvEntry.toString());
    }

}
