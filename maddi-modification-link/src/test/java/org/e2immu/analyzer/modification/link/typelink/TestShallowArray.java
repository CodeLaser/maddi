package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
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
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);
        tlc.doPrimaryType(C);

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables tlv1 = method1.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*[Type param X]:0[Type param X[]])", tlv1.toString());

        MethodInfo method2 = C.findUniqueMethod("method2", 1);
        MethodLinkedVariables tlv2 = method2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*[Type param X]:[0]0[Type param X[][]])", tlv2.toString());

        MethodInfo method3 = C.findUniqueMethod("method3", 1);
        MethodLinkedVariables tlv3 = method3.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*[Type param X[]]:0[Type param X[][]])", tlv3.toString());

        MethodInfo method4 = C.findUniqueMethod("method4", 1);
        MethodLinkedVariables tlv4 = method4.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(0[Type param X[][]]:*[Type param X[]])", tlv4.toString());
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
                public static <X> X[][] method4(List<X[]> x) {
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
        tlc.doPrimaryType(C);
        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables tlv1 = method1.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*[Type param X]:[0]0[Type java.util.List<X[]>])", tlv1.toString());

        MethodInfo method2 = C.findUniqueMethod("method2", 1);
        MethodLinkedVariables tlv2 = method2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*[Type param X]:[0,0]0[Type java.util.List<X[][]>])", tlv2.toString());

        MethodInfo method3 = C.findUniqueMethod("method3", 1);
        MethodLinkedVariables tlv3 = method3.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x(*[Type param X[]]:[0]0[Type java.util.List<X[][]>])", tlv3.toString());

        MethodInfo method4 = C.findUniqueMethod("method4", 1);
        MethodLinkedVariables tlv4 = method4.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        //! we cannot find a T[][] in List<T[]>, which is correct!!
        assertEquals("", tlv4.toString());
    }
}
