package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// the @Override supertype is a sibling of the frame, so the frame-nested types it references in the reproduced
// signature must be qualified with the frame name; inside the frame those same types stay simple
public class TestIsolateMethod13OverrideQualified extends CommonIsolateMethodTest {

    @Language("java")
    public static final String O1 = """
            package a.b;
            class ST { }
            class Q { }
            interface I { Q cc(ST token); }
            public class X implements I {
                @Override
                public Q cc(ST token) {
                    return null;
                }
            }
            """;

    @DisplayName("@Override supertype qualifies frame-nested types in the reproduced signature")
    @Test
    public void o1() {
        TypeInfo x = parse("a.b.X", O1);
        String m = """
                @Override
                public Q cc(ST token) {
                    return null;
                }""";
        String out = isolate(x, "cc", 1, m);
        @Language("java")
        String expected = """
                abstract class X_cc_super { abstract X_cc.Q cc(X_cc.ST token); } public class X_cc extends X_cc_super {
                    class Q { }
                    class ST { }
                    @Override
                public Q cc(ST token) {
                    return null;
                }
                }
                """;
        assertEquals(expected, out);
        // NOTE: this output compiles with javac, but maddi's parser cannot reparse it: the supertype refers to a
        // sibling type's nested member by qualified name ('X_cc.Q'), and the scanner stubs the
        // 'X_cc' prefix and then reports a duplicate when the real type is defined. So no reparse assertion here.
    }
}
