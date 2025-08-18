package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitch extends CommonTest {


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class X {
                int method(Object o) {
                    switch(o) {
                        case String s:  return s.length();
                        case List<?> list: return list.size();
                        case int i when i > 10: return i;
                        default: throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            class X {
                record R(int i, int j) { }
                record S(String s, R r) { }
                int method(Object o) {
                    switch(o) {
                        case R(int i, int j): return i + j;
                        case S(String s, R(int i, int j)): return s.length() - i; 
                        default: throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.List;
            class X {
                int method(Object o) {
                    switch(o) {
                        case String s -> { return s.length(); }
                        case List<?> list -> { return list.size(); }
                        case int i when i > 10 -> { return i; }
                        default -> throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        SwitchStatementNewStyle ns = (SwitchStatementNewStyle) methodInfo.methodBody().lastStatement();
        SwitchEntry s0 = ns.entries().getFirst();
        assertEquals("String s", s0.patternVariable().toString());

        SwitchEntry s1 = ns.entries().get(1);
        assertEquals("List<?> list", s1.patternVariable().toString());
        assertEquals("<empty>", s1.whenExpression().toString());
        assertEquals("7-13:7-56", s1.source().compact2());
        assertEquals("7-18:7-29", s1.patternVariable().source().compact2());

        SwitchEntry s2 = ns.entries().get(2);
        assertEquals("int i", s2.patternVariable().toString());
        assertEquals("i>10", s2.whenExpression().toString());
        assertEquals("8-13:8-51", s2.source().compact2());
        assertEquals("8-18:8-22", s2.patternVariable().source().compact2());
        assertEquals("8-29:8-34", s2.whenExpression().source().compact2());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.List;
            class X {
                int method(Object o) {
                    return switch(o) {
                        case String s -> s.length();
                        case List<?> list -> list.size();
                        case int i when i > 10 -> i;
                        case null, default -> 0;
                    };
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.List;
            class X {
                record R(int i, int j) { }
                record S(String s, R r) { }
                int method(Object o) {
                    switch(o) {
                        case R(int i, int j) -> { return i + j; }
                        case S(String s, R(int i, int j)) -> { return s.length() - i; }
                        default -> throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test6() {
        TypeInfo typeInfo = javaInspector.parse(INPUT6, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        SwitchStatementNewStyle ns = (SwitchStatementNewStyle) methodInfo.methodBody().lastStatement();
        SwitchEntry s0 = ns.entries().getFirst();
        assertEquals("R(int i,int j)", s0.patternVariable().toString());
        SwitchEntry s1 = ns.entries().get(1);
        assertEquals("S(String s,R(int i,int j))", s1.patternVariable().toString());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.List;
            class X {
                record R(int i, int j) { }
                record S(String s, R r) { }
                int method(Object o) {
                    return switch(o) {
                        case R(int i, int j) -> i + j;
                        case S(String s, R(int i, int j)) -> s.length() - i;
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }
}