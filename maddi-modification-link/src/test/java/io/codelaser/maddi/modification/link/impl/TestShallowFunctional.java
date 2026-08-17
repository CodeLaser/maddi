package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.common.defaults.ShallowAnalyzer;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.link.vf.VirtualFieldComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.INDEPENDENT_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallowFunctional extends CommonTest {

    // any functional interface will show the same behaviour

    @DisplayName("Analyze 'BiFunction'")
    @Test
    public void test5() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo stream = javaInspector.compiledTypesManager().type(BiFunction.class);
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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
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
