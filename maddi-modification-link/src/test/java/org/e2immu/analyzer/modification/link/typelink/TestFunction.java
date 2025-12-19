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
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestFunction extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.util.Optional;
            public class C<X> { 
                public X method(Optional<List<X>> optional) {
                    Optional<X> optX = optional.map(List::getFirst);
                    return optX.orElseThrow();
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

        MethodInfo method = C.findUniqueMethod("method", 1);
        TypeInfo optional = javaInspector.compiledTypesManager().get(Optional.class);
        MethodInfo map = optional.findUniqueMethod("map", 1);
        MethodLinkedVariables tlvMap = map.analysis().getOrNull(METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertEquals("""
                return map(0[Type java.util.Optional<U>]:1[Type java.util.function.Function<? super T,? extends U>])\
                [#0:return map(0[Type java.util.function.Function<? super T,? extends U>]:0[Type java.util.Optional<T>])]\
                """, tlvMap.toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("optX");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                optional(0[Type java.util.Optional<X>]:[0]0[Type java.util.Optional<java.util.List<X>>])\
                """, tlvX.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            import java.util.Optional;
            public class C<X, Y> {
                public Map.Entry<X, Y> method(Optional<List<Map.Entry<X, Y>>> optional) {
                    Optional<Map.Entry<X, Y>> optXY = optional.map(List::getFirst);
                    return optXY.orElseThrow();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = C.findUniqueMethod("method", 1);
        tlc.doPrimaryType(C);

        // IMPORTANT: just as in TestSupplier,2, the Map.Entry<X,Y> is seen as a single type
        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viX0 = vd0.variableInfo("optXY");
        Links tlvX = viX0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                optional(0[Type java.util.Optional<java.util.Map.Entry<X,Y>>]:\
                [0]0[Type java.util.Optional<java.util.List<java.util.Map.Entry<X,Y>>>])\
                """, tlvX.toString());
    }
}
