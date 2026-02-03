package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallowFunctional extends CommonTest {

    // any functional interface will show the same behaviour

    @DisplayName("Analyze 'BiFunction'")
    @Test
    public void test5() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(BiFunction.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("/ - /", vfc.compute(stream).toString());

        MethodInfo apply = stream.findUniqueMethod("apply", 2);
        MethodLinkedVariables mlvFindFirst = linkComputer.doMethod(apply);
        assertEquals("[-, -] --> -", mlvFindFirst.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.function.Function;
            public class X {
                @FunctionalInterface
                interface Invalidated extends Function<Integer, String> {
                }
                interface ParseOptionsBuilder {
                    ParseOptionsBuilder setDetailedSources(boolean detailedSources);
                    ParseOptionsBuilder setInvalidated(Invalidated invalidated);
                }
            }
            """;

    @DisplayName("find return type")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, false);
        shallowAnalyzer.go(List.of(X));

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector, LinkComputer.Options.FORCE_SHALLOW);
        linkComputer.doPrimaryType(X);
        TypeInfo pob = X.findSubType("ParseOptionsBuilder");
        MethodInfo setInvalidated = pob.findUniqueMethod("setInvalidated", 1);
        MethodLinkedVariablesImpl mlv = setInvalidated.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                [-] --> setInvalidated.§$←this*.§$,setInvalidated.§m≡this*.§m\
                """, mlv.toString());
        Variable from0 =  mlv.ofReturnValue().link(0).from();
        assertEquals("Type a.b.X.ParseOptionsBuilder", from0.parameterizedType().toString());
    }

}
