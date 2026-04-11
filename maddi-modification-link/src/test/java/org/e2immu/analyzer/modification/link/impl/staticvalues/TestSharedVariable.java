package org.e2immu.analyzer.modification.link.impl.staticvalues;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
This test is created to show the need for grouping shared variables.
The 'r' parameter of type 'R' here is shared among 3 different 'C' objects.
Each of them is retained because it becomes part of the return value.
Because R itself has fields, and it is embedded, a dense graph is generated, with a size at quadratic
in the product of amount of relevant fields, return points, etc.
 */
public class TestSharedVariable extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;import java.util.Set;
            class X {
                record R(List<String> list1, List<String> list2) {}
                record S(String s1, String s2) {}
                record C(R r, int i) {
                    S choose() {
                        return new S(r.list1.get(i), r.list2.get(i));
                    }
                }
                public S method(R r, String k) {
                    if("?".equals(k)) {
                        C c1 = new C(r, 1);
                        return c1.choose();
                    }
                    if("!".equals(k)) {
                        C c2 = new C(r, 2);
                        return c2.choose();
                    }
                    C c3 = new C(r, 3);
                    return c3.choose();
                }
            }
            """;

    @DisplayName("direct assignment")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        TypeInfo C = X.findSubType("C");
        MethodInfo choose = C.findUniqueMethod("choose", 0);
        MethodLinkedVariables mlvChoose = choose.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(choose));
        assertEquals("""
                [] --> choose.s1∈this.r.list1.§$s,choose.s2∈this.r.list2.§$s\
                """, mlvChoose.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("""
                """, mlv.toString());
    }
}
