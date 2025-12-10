package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.*;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestAssignmentIdentityMethod extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X<T> {
                private static <T> T notNull(T t) {
                    assert t != null;
                    return t;
                }
                public T method(T t) {
                    T tt = t;
                    T ttt= notNull(tt);
                    return ttt;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        MethodInfo notNull = X.findUniqueMethod("notNull", 1);
        ParameterInfo p0 = notNull.parameters().getFirst();
        ReturnVariable rv = new ReturnVariableImpl(notNull);
        notNull.analysis().set(METHOD_LINKS,
                new MethodLinkedVariablesImpl(new LinksImpl.Builder(rv).add(LinkNature.IS_IDENTICAL_TO, p0).build(),
                        List.of()));

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        tlc.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo tt0 = vd0.variableInfo("tt");
        Links tlvTt0 = tt0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("t(*:*)", tlvTt0.toString());

        // does the value get carried over to the next statement?

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo tt1 = vd1.variableInfo("tt");
        Links tlvTt1 = tt1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("t(*:*)", tlvTt1.toString());

        // now look at ttt, result of @Identity

        VariableInfo ttt1 = vd1.variableInfo("ttt");
        Links tlvTtt1 = ttt1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("t(*:*);tt(*:*)", tlvTtt1.toString());

        // NOTE: this is different from the shallow one; but has the same meaning
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("t(*:*)", tlvMethod.toString());
    }

    @Test
    public void testShallow() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, true);
        tlc.doPrimaryType(X);

        MethodInfo notNull = X.findUniqueMethod("notNull", 1);
        MethodLinkedVariables tlvNotNull = notNull.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("t(*:*)", tlvNotNull.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("return method(*:0)[#0:return method(*:0)]", tlvMethod.toString());
    }
}
