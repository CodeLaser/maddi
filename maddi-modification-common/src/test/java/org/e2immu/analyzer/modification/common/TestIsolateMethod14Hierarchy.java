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

    @Language("java")
    public static final String INPUT4 = """
            package a.b;
            import java.io.Serializable;
            class LongVector implements Serializable, Iterable<Long> {
                boolean add(long o) { return false; }
                long[] toArray() { return null; }
                public java.util.Iterator<Long> iterator() { return null; }
            }
            public class X {
                Object method() {
                    LongVector v = new LongVector();
                    v.add(1L);
                    return v.toArray();
                }
            }
            """;

    @DisplayName("instantiated stub implementing an interface gets dummy implementations of its abstract methods")
    @Test
    public void test4() {
        TypeInfo x = parse("a.b.X", INPUT4);
        String m = """
                Object method() {
                    LongVector v = new LongVector();
                    v.add(1L);
                    return v.toArray();
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                import java.io.Serializable;
                import java.util.Iterator;
                public class X_method {
                    class LongVector implements Serializable, Iterable<Long> {
                        LongVector() { }
                        boolean add(long o) { return false; }
                        long [] toArray() { return null; }
                        public Iterator<Long> iterator() { return null; }
                    }

                    Object method() {
                    LongVector v = new LongVector();
                    v.add(1L);
                    return v.toArray();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            class IDataType { }
            class ObjectID extends IDataType {
                public String toString() { return null; }
                ObjectID(long objectID) { }
                String getDefaultValue() { return null; }
            }
            public class X {
                String method(ObjectID id) {
                    return id.toString() + id.getDefaultValue();
                }
            }
            """;

    @DisplayName("a stub method overriding java.lang.Object (toString) must be public")
    @Test
    public void test5() {
        TypeInfo x = parse("a.b.X", INPUT5);
        String m = """
                String method(ObjectID id) {
                    return id.toString() + id.getDefaultValue();
                }""";
        String out = isolate(x, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class IDataType { }
                    class ObjectID extends IDataType {
                        public String toString() { return null; }
                        String getDefaultValue() { return null; }
                    }

                    String method(ObjectID id) {
                    return id.toString() + id.getDefaultValue();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT6 = """
            package a.b;
            public class X {
                Object method() {
                    ArrayList<String> list = new ArrayList<>();
                    list.add("x");
                    return list;
                }
            }
            class ArrayList<I> extends java.util.ArrayList<I> {
                public boolean add(I o) { return false; }
            }
            """;

    @DisplayName("a stub method overriding an inherited interface method (Collection.add) must be public")
    @Test
    public void test6() {
        TypeInfo x = parse("a.b.X", INPUT6);
        String m = """
                Object method() {
                    ArrayList<String> list = new ArrayList<>();
                    list.add("x");
                    return list;
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class ArrayList<I> extends java.util.ArrayList<I> {ArrayList() { }public boolean add(I o) { return false; } }
                    Object method() {
                    ArrayList<String> list = new ArrayList<>();
                    list.add("x");
                    return list;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT7 = """
            package a.b;
            import java.io.Serializable;
            public class X {
                static class Node implements Comparable<Node>, Cloneable, Serializable {
                    public int compareTo(Node o) { return 0; }
                }
                Node method() {
                    Node d = new Node();
                    return d;
                }
            }
            """;

    @DisplayName("a self-referential nested type (Node implements Comparable<Node>) is stubbed exactly once")
    @Test
    public void test7() {
        TypeInfo x = parse("a.b.X", INPUT7);
        String m = """
                Node method() {
                    Node d = new Node();
                    return d;
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                import java.io.Serializable;
                public class X_method {
                    class Node implements Comparable<Node>, Cloneable, Serializable {
                        Node() { }
                        public int compareTo(Node arg0) { return 0; }
                    }

                    Node method() {
                    Node d = new Node();
                    return d;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT8 = """
            package a.b;
            public class X {
                interface Sink { void accept(IDataType t); }
                static class IDataType implements Comparable<IDataType> {
                    public int compareTo(IDataType o) { return 0; }
                }
                static class Holder implements Sink {
                    public void accept(IDataType t) { }
                }
                Object method() {
                    Holder h = new Holder();
                    return h;
                }
            }
            """;

    @DisplayName("a type first stubbed inside the dummy-implementation pass also receives its own dummy methods")
    @Test
    public void test8() {
        TypeInfo x = parse("a.b.X", INPUT8);
        String m = """
                Object method() {
                    Holder h = new Holder();
                    return h;
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class Holder implements Sink {Holder() { }public void accept(IDataType t) { } }
                    class IDataType implements Comparable<IDataType> {public int compareTo(IDataType arg0) { return 0; } }
                    interface Sink { }
                    Object method() {
                    Holder h = new Holder();
                    return h;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
