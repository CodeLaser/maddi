package org.e2immu.language.inspection.integration.java.other;


import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestRecordPattern extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X1 {
                interface I { }
                record RI(String s) implements I { }
                void method(I i) {
                    if(i instanceof RI(String t)) {
                        System.out.println(t);
                    }
                }
            }
            """;

    @DisplayName("record pattern 1, basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = X.findUniqueMethod("method", 1);
        InstanceOf instanceOf = (InstanceOf) methodInfo.methodBody().statements().getFirst().expression();
        RecordPattern recordPattern = instanceOf.patternVariable();
        assertNotNull(recordPattern);
        assertEquals("6-25:6-36", instanceOf.source().detailedSources().detail(recordPattern).compact2());
        RecordPattern p0 = recordPattern.patterns().getFirst();
        assertEquals("6-28:6-35", recordPattern.source().detailedSources().detail(p0).compact2());
        LocalVariable lv = p0.localVariable();
        assertEquals("6-28:6-35", p0.source().detailedSources().detail(lv).compact2());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X2 {
                interface I<T> { }
                record RI<T>(T t) implements I<T> { }
                void method(I<String> i) {
                    if(i instanceof RI<String>(String s)) {
                        System.out.println(s);
                    }
                }
            }
            """;

    @DisplayName("record pattern 2, type parameter")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

    }

    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            class X2 {
                interface I<T> { }
                record RI<T>(T t) implements I<T> { }
                void method(I<String> i) {
                    if(i instanceof RI<String>(var s)) {
                        System.out.println(s);
                    }
                }
            }
            """;

    @DisplayName("record pattern 2, type parameter and var")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2b);
        MethodInfo methodInfo = X.findUniqueMethod("method", 1);
        InstanceOf instanceOf = (InstanceOf) methodInfo.methodBody().statements().getFirst().expression();
        RecordPattern recordPattern = instanceOf.patternVariable();
        RecordPattern s = recordPattern.patterns().getFirst();
        assertEquals("s", s.localVariable().simpleName());
        assertEquals("String", s.localVariable().parameterizedType().fullyQualifiedName());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X3 {
                interface I { }
                record RI(int j) implements I { }
                void method(I i) {
                    if(i instanceof RI(int k)) {
                        System.out.println(k);
                    }
                }
            }
            """;

    @DisplayName("record pattern 3, primitive type")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X4 {
                record Point(int x, int y) {}
                enum Color { RED, GREEN, BLUE }
                record ColoredPoint(Point p, Color c) {}
                record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
                static void printUpperLeftColoredPoint(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint ul, ColoredPoint lr)) {
                         System.out.println(ul.c());
                    }
                }
                static void printColorOfUpperLeftPoint2(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point p, Color c),
                                               ColoredPoint lr)) {
                        System.out.println(c);
                    }
                }
            }
            """;

    @DisplayName("record pattern 4, nested")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);

    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class X5 {
                record Point(int x, int y) {}
                enum Color { RED, GREEN, BLUE }
                record ColoredPoint(Point p, Color c) {}
                record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
                static void printXCoordOfUpperLeftPointWithPatterns(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point(int x, int _), Color c), _)) {
                         System.out.println("Upper-left corner: " + x);
                    }
                }
            }
            """;

    @DisplayName("record pattern 5, unnamed")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);

    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            class X5 {
                record Point(int x, int y) {}
                enum Color { RED, GREEN, BLUE }
                record ColoredPoint(Point p, Color c) {}
                record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
                static void printXCoordOfUpperLeftPointWithPatterns(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point(var x, var y), var c), var lr)) {
                         System.out.println("Upper-left corner: " + x);
                    }
                }
                static void printXCoordOfUpperLeftPointWithPatterns2(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point(var x, var y), var c), _)) {
                         System.out.println("Upper-left corner: " + x);
                    }
                }
            }
            """;

    @DisplayName("record pattern 6, var")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        MethodInfo methodInfo = X.findUniqueMethod("printXCoordOfUpperLeftPointWithPatterns", 1);
        InstanceOf instanceOf = (InstanceOf) methodInfo.methodBody().statements().getFirst().expression();
        RecordPattern recordPattern = instanceOf.patternVariable();
        RecordPattern lr = recordPattern.patterns().getLast();
        assertEquals("a.b.X5.ColoredPoint", lr.localVariable().parameterizedType().fullyQualifiedName());
        RecordPattern cp = recordPattern.patterns().getFirst();
        RecordPattern point = cp.patterns().getFirst();
        RecordPattern x = point.patterns().getFirst();
        assertEquals("int", x.localVariable().parameterizedType().fullyQualifiedName());
        assertEquals("x", x.localVariable().simpleName());
        RecordPattern color = cp.patterns().getLast();
        assertEquals("a.b.X5.Color", color.localVariable().parameterizedType().fullyQualifiedName());
        assertEquals("c", color.localVariable().fullyQualifiedName());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            class X {
                record Point(int x, int y) {}
                int method(Object obj) {
                   if(obj instanceof Point(var x, var y)) {
                       return x + y;
                   }
                   return 0;
                }
            }
            """;

    @DisplayName("record pattern 5, unnamed")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = X.findUniqueMethod("method", 1);
        InstanceOf instanceOf = (InstanceOf) methodInfo.methodBody().statements().getFirst().expression();
        RecordPattern point = instanceOf.patternVariable();
        RecordPattern x = point.patterns().getFirst();
        assertEquals("5-32:5-36", x.source().compact2());
        assertEquals("5-36:5-36", x.source().detailedSources().detail(x.localVariable().fullyQualifiedName()).compact2());
    }

}


