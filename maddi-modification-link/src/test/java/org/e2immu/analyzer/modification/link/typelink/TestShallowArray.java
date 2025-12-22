package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallowArray extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class C {
                public static <X> X method1(X[] x) {
                    return x[0];
                }
                public static <X> X method2(X[][] x) {
                    return x[0][0];
                }
                public static <X> X[] method3(X[][] x) {
                    return x[0];
                }
                public static <X> X[][] method4(X[] x) {
                    return null;
                }
                public static <X> X[][][] method5(X[] x) {
                    return null;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method1));
        assertEquals("[-] --> method1∈0:x.§xs", mlv1.toString());

        MethodInfo method2 = C.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method2));
        assertEquals("[-] --> method2∈∈0:x.§xss", mlv2.toString());

        MethodInfo method3 = C.findUniqueMethod("method3", 1);
        MethodLinkedVariables mlv3 = method3.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method3));
        assertEquals("[-] --> method3.§xs∈0:x.§xss", mlv3.toString());

        MethodInfo method4 = C.findUniqueMethod("method4", 1);
        MethodLinkedVariables mlv4 = method4.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method4));
        assertEquals("[-] --> method4.§xss∋0:x.§xs", mlv4.toString());

        MethodInfo method5 = C.findUniqueMethod("method5", 1);
        MethodLinkedVariables mlv5 = method5.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method5));
        assertEquals("[-] --> method5.§xsss∋∋0:x.§xs", mlv5.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            public class C {
                public static <X> X method1(List<X[]> x) {
                    return null;
                }
                public static <X> X method2(List<X[][]> x) {
                    return null;
                }
                public static <X> X[] method3(List<X[][]> x) {
                    return null;
                }
                public static <X> X[] method4(List<X[]> x) {
                    return null;
                }
                public static <X> X[][] method5(List<X[]> x) {
                    return null;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method1));
        assertEquals("[-] --> method1∈∈0:x.§xss", mlv1.toString());

        MethodInfo method2 = C.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method2));
        assertEquals("[-] --> method2∈∈∈0:x.§xsss", mlv2.toString());

        MethodInfo method3 = C.findUniqueMethod("method3", 1);
        MethodLinkedVariables mlv3 = method3.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method3));
        assertEquals("[-] --> method3.§xs∈∈0:x.§xsss", mlv3.toString());

        MethodInfo method4 = C.findUniqueMethod("method4", 1);
        MethodLinkedVariables mlv4 = method4.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method4));
        assertEquals("[-] --> method4.§xs∈0:x.§xss", mlv4.toString());

        MethodInfo method5 = C.findUniqueMethod("method5", 1);
        MethodLinkedVariables mlv5 = method5.analysis().getOrCreate(METHOD_LINKS, ()-> tlc.doMethod(method5));
        assertEquals("[-] --> method5.§xss~0:x.§xss", mlv5.toString());
    }
}
