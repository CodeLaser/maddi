package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.*;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
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
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options(false, false, true));
        MethodInfo notNull = X.findUniqueMethod("notNull", 1);
        MethodLinkedVariables mlvNotNull =notNull.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(notNull));
        assertEquals("[-] --> notNull←0:t", mlvNotNull.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables tlvMethod = tlc.doMethod(method);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo tt0 = vd0.variableInfo("tt");
        Links tlvTt0 = tt0.linkedVariablesOrEmpty();
        assertEquals("tt←0:t", tlvTt0.toString());

        // does the value get carried over to the next statement?

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo tt1 = vd1.variableInfo("tt");
        Links tlvTt1 = tt1.linkedVariablesOrEmpty();
        assertEquals("tt→ttt,tt←0:t", tlvTt1.toString());

        // now look at ttt, result of @Identity

        VariableInfo ttt1 = vd1.variableInfo("ttt");
        Links tlvTtt1 = ttt1.linkedVariablesOrEmpty();
        assertEquals("ttt←0:t,ttt←tt", tlvTtt1.toString());

        // NOTE: this is different from the shallow one; but has the same meaning
        assertEquals("[-] --> method←0:t", tlvMethod.toString());
    }

    @Test
    public void testShallow() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options(false, true, true));
        tlc.doPrimaryType(X);

        MethodInfo notNull = X.findUniqueMethod("notNull", 1);
        MethodLinkedVariables tlvNotNull = notNull.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> notNull←0:t", tlvNotNull.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:t→this*.§t] --> method←this*.§t", tlvMethod.toString());
    }
}
