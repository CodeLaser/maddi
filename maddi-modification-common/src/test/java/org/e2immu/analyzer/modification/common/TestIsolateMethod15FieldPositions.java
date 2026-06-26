package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Coverage for field references in many syntactic positions: every accessed field of a stubbed type must be
// reproduced, regardless of where the access appears. This is a scaffold -- add the position that currently loses a
// field (a stub ending up with fewer fields than are accessed) as another line in 'method' below.
public class TestIsolateMethod15FieldPositions extends CommonIsolateMethodTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            import java.util.function.Supplier;
            public class X {
                static class Sub { int value; }
                static class Obj {
                    int a, b, c, d, e, f, g, h, i, j, k, l, m;
                    Sub sub;
                    Runnable r;
                }
                void sink(int v) { }
                int method(Obj o, Obj[] arr, int idx) {
                    o.a = 1;                                                   // assignment target
                    int x = o.b;                                              // read
                    o.c += 2;                                                 // compound assignment
                    o.d++;                                                    // post-increment
                    --o.e;                                                    // pre-decrement
                    sink(o.f);                                               // method argument
                    if (o.g > 0) { }                                         // condition
                    int t = o.h > 0 ? o.i : o.j;                             // ternary (cond/then/else)
                    Supplier<Integer> s = () -> o.k;                         // inside a lambda
                    Runnable run = new Runnable() {                          // inside an anonymous class
                        public void run() { sink(o.l); }
                    };
                    arr[idx].m = 7;                                          // field on an array-indexed scope
                    o.sub.value = 5;                                         // field as the scope of another field
                    o.r.run();                                              // field as the scope of a method call
                    s.get();
                    run.run();
                    return t + x;
                }
            }
            """;

    @DisplayName("a field accessed in any position is reproduced on its stub")
    @Test
    public void test() {
        TypeInfo x = parse("a.b.X", INPUT);
        String m = """
                int method(Obj o, Obj[] arr, int idx) {
                    o.a = 1;
                    int x = o.b;
                    o.c += 2;
                    o.d++;
                    --o.e;
                    sink(o.f);
                    if (o.g > 0) { }
                    int t = o.h > 0 ? o.i : o.j;
                    Supplier<Integer> s = () -> o.k;
                    Runnable run = new Runnable() {
                        public void run() { sink(o.l); }
                    };
                    arr[idx].m = 7;
                    o.sub.value = 5;
                    o.r.run();
                    s.get();
                    run.run();
                    return t + x;
                }""";
        String out = isolate(x, "method", 3, m);
        // every accessed field of Obj (a..m, sub, r) plus Sub.value must appear on a stub
        for (String field : new String[]{"int a;", "int b;", "int c;", "int d;", "int e;", "int f;", "int g;",
                "int h;", "int i;", "int j;", "int k;", "int l;", "int m;", "Sub sub;", "Runnable r;", "int value;"}) {
            assertTrue(out.contains(field), "missing field '" + field + "' in:\n" + out);
        }
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INHERITED = """
            package a.b;
            import java.io.Serializable;
            public class X {
                static class PeriodData implements Serializable {
                    public double residualValue;
                    public double capitalPortion;
                }
                static class SpecialData extends PeriodData {
                    public int number;
                }
                void method(SpecialData pp, PeriodData pd) {
                    pp.residualValue = 1.0;
                    pp.number = 3;
                    pd.capitalPortion = 2.0;
                    double x = pd.residualValue;
                }
            }
            """;

    // an inherited field accessed via a subtype belongs on the supertype stub (the subtype inherits it), even when
    // the same field is also accessed via the supertype: it must not be 'claimed' by the subtype and then lost
    @DisplayName("inherited field accessed via a subtype is reproduced on its declaring supertype")
    @Test
    public void inherited() {
        TypeInfo x = parse("a.b.X", INHERITED);
        String m = """
                void method(SpecialData pp, PeriodData pd) {
                    pp.residualValue = 1.0;
                    pp.number = 3;
                    pd.capitalPortion = 2.0;
                    double x = pd.residualValue;
                }""";
        String out = isolate(x, "method", 2, m);
        @Language("java")
        String expected = """
                import java.io.Serializable;
                public class X_method {
                    class PeriodData implements Serializable { double capitalPortion; double residualValue; }
                    class SpecialData extends PeriodData { int number; }
                    void method(SpecialData pp, PeriodData pd) {
                    pp.residualValue = 1.0;
                    pp.number = 3;
                    pd.capitalPortion = 2.0;
                    double x = pd.residualValue;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String SELF_STATIC = """
            package a.b;
            public class C {
                public static final int DAYS = 10;
                int method() {
                    return C.DAYS;
                }
            }
            """;

    // the frame is renamed (C -> C_method), so a self-reference written with the original name ('C.DAYS') needs a
    // stub carrying that name nested in the frame, or the verbatim text no longer resolves
    @DisplayName("a static field referenced through the original type's own name keeps resolving")
    @Test
    public void selfStatic() {
        TypeInfo x = parse("a.b.C", SELF_STATIC);
        String m = """
                int method() {
                    return C.DAYS;
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                public class C_method {
                    class C { static final int DAYS = 0; }
                    int method() {
                    return C.DAYS;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("C_method", out));
    }

    @Language("java")
    public static final String SELF_MEMBERS = """
            package a.b;
            import java.util.function.IntSupplier;
            public class C {
                public static final int DAYS = 10;
                static int other() { return 2; }
                int instanceVal() { return 3; }
                int method(C param) {
                    int b = C.other();
                    int d = C.DAYS;
                    C local = new C();
                    int e = param.instanceVal();
                    IntSupplier s = C::other;
                    return b + d + e + local.instanceVal() + s.getAsInt();
                }
            }
            """;

    // the original type referenced by its own name -- 'C.other()', 'C.DAYS', 'new C()', a 'C' parameter/local, and
    // 'C::other' -- all resolve to the stub bearing that name; an unqualified self-call would stay on the frame
    @DisplayName("self-references through the original type's name (static method, new, parameter, method ref)")
    @Test
    public void selfMembers() {
        TypeInfo x = parse("a.b.C", SELF_MEMBERS);
        String m = """
                int method(C param) {
                    int b = C.other();
                    int d = C.DAYS;
                    C local = new C();
                    int e = param.instanceVal();
                    java.util.function.IntSupplier s = C::other;
                    return b + d + e + local.instanceVal() + s.getAsInt();
                }""";
        String out = isolate(x, "method", 1, m);
        @Language("java")
        String expected = """
                import java.util.function.IntSupplier;
                public class C_method {
                    class C { static final int DAYS = 0;static int other() { return 0; }C() { }int instanceVal() { return 0; } }
                    int method(C param) {
                    int b = C.other();
                    int d = C.DAYS;
                    C local = new C();
                    int e = param.instanceVal();
                    java.util.function.IntSupplier s = C::other;
                    return b + d + e + local.instanceVal() + s.getAsInt();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("C_method", out));
    }
}
