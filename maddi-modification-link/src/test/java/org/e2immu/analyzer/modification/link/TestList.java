package org.e2immu.analyzer.modification.link;

import org.e2immu.analyzer.modification.link.impl.*;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinkedVariablesImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestList extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X<T> {
                T[] ts;
                private T get(int index) {
                    return ts[index];
                }
                public static <K> K method(int i, X<K> x) {
                    K k = x.get(i);
                    return k;
                }
            }
            """;

    @DisplayName("Analyze 'get', array access")
    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(get);
        assertEquals("get<this.ts", mlv.ofReturnValue().toString());
    }
    
    @DisplayName("Analyze 'method', given method links for 'get'")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        FieldInfo ts = X.getFieldByName("ts", true);
        MethodInfo get = X.findUniqueMethod("get", 1);
        ReturnVariable rv = new ReturnVariableImpl(get);
        Links rvLinks = new LinksImpl(List.of(new LinkImpl(rv, LinkNature.IS_ELEMENT_OF, runtime.newFieldReference(ts))));
        assertEquals("get<this.ts", rvLinks.toString());
        get.analysis().set(METHOD_LINKS, new MethodLinkedVariablesImpl(rvLinks, List.of()));

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        tlc.doPrimaryType(X);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo k0 = vd0.variableInfo("k");
        LinkedVariables tlvK0 = k0.analysis().getOrDefault(LINKS, LinkedVariablesImpl.EMPTY);
        assertEquals("x(*:0)", tlvK0.toString());
        assertEquals("x(*[Type param K]:0[Type a.b.X<K>])", tlvK0.toString());

        MethodLinkedVariables lvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*:0)", lvMethod.toString());
        assertEquals("x(*[Type param K]:0[Type a.b.X<K>])", lvMethod.toString());
    }

}
