package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// stub types must reproduce the supertype hierarchy: overload resolution (isEmpty(IBase) vs isEmpty(ISub)) and a
// generic bound (T extends IBase accepting an ISub argument) only resolve when ISub is a subtype of IBase
public class TestIsolateMethod14Hierarchy extends CommonIsolateMethodTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            interface IBase { }
            interface ISub extends IBase { }
            class Util {
                static boolean isEmpty(IBase p) { return false; }
                static boolean isEmpty(ISub p) { return false; }
                static <T extends IBase> T add(T a, T b, boolean unique) { return null; }
            }
            public class X {
                ISub method(IBase base, ISub sub) {
                    boolean e1 = Util.isEmpty(base);
                    boolean e2 = Util.isEmpty(sub);
                    return Util.add(sub, sub, e1 && e2);
                }
            }
            """;

    @DisplayName("stub hierarchy enables overload resolution and a generic bound")
    @Test
    public void test() {
        TypeInfo x = parse("a.b.X", INPUT);
        String m = """
                ISub method(IBase base, ISub sub) {
                    boolean e1 = Util.isEmpty(base);
                    boolean e2 = Util.isEmpty(sub);
                    return Util.add(sub, sub, e1 && e2);
                }""";
        String out = isolate(x, "method", 2, m);
        @Language("java")
        String expected = """
                public class X_method {
                    interface IBase { }
                    interface ISub extends IBase { }
                    class Util {
                        static boolean isEmpty(IBase p) { return false; }
                        static boolean isEmpty(ISub p) { return false; }
                        static <T extends IBase> T add(T a, T b, boolean unique) { return null; }
                    }

                    ISub method(IBase base, ISub sub) {
                    boolean e1 = Util.isEmpty(base);
                    boolean e2 = Util.isEmpty(sub);
                    return Util.add(sub, sub, e1 && e2);
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            import java.io.Serializable;
            interface IMarker { }
            class Holder implements Serializable, IMarker { }
            public class X {
                Holder method() {
                    return null;
                }
            }
            """;

    @DisplayName("a type implementing several interfaces lists each once")
    @Test
    public void test2() {
        TypeInfo x = parse("a.b.X", INPUT2);
        String m = """
                Holder method() {
                    return null;
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                import java.io.Serializable;
                public class X_method {
                    class Holder implements Serializable, IMarker { }
                    interface IMarker { }
                    Holder method() {
                    return null;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            public class X {
                Object method() {
                    ArrayList list = new ArrayList();
                    return list.get(0);
                }
            }
            class ArrayList extends java.util.ArrayList<String> { }
            """;

    @DisplayName("custom type whose simple name clashes with its JDK supertype (ArrayList extends java.util.ArrayList)")
    @Test
    public void test3() {
        TypeInfo x = parse("a.b.X", INPUT3);
        String m = """
                Object method() {
                    ArrayList list = new ArrayList();
                    return list.get(0);
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class ArrayList extends java.util.ArrayList<String> {ArrayList() { } }
                    Object method() {
                    ArrayList list = new ArrayList();
                    return list.get(0);
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
