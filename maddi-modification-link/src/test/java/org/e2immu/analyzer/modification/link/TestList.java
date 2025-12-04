package org.e2immu.analyzer.modification.link;

import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
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
        assertEquals("get==this.ts[index],get<this.ts", mlv.ofReturnValue().toString());
    }

    @DisplayName("Analyze 'method', given method links for 'get'")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 1);
        MethodInfo method = X.findUniqueMethod("method", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        // first, do get()
        MethodLinkedVariables lvGet = get.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(get));
        assertEquals("get==this.ts[index],get<this.ts", lvGet.ofReturnValue().toString());

        // then, do method
        MethodLinkedVariables lvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo k0 = vd0.variableInfo("k");
        Links linksK = k0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("k==x.ts[index],k<x.ts", linksK.toString());

        assertEquals("method=x.ts[index],method<x.ts", lvMethod.toString());
    }

}
