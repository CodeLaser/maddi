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
}
