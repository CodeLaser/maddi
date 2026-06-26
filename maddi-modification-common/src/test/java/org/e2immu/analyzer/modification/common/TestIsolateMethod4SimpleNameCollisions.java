package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestIsolateMethod4SimpleNameCollisions extends CommonIsolateMethodTest {

    // a nested 'Logger' (referenced simply) and the external org.slf4j.Logger (written fully qualified): both
    // would stub to a simple 'Logger' and collide. The qualified one must keep resolving via a reproduced package.
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            public class X {
                static class Logger { String tag; }
                String method(org.slf4j.Logger ext, Logger inner) {
                    return ext.getName() + inner.tag;
                }
            }
            """;

    @DisplayName("simple-name collision: nested Logger vs fully-qualified org.slf4j.Logger")
    @Test
    public void test1() {
        TypeInfo X = parse("a.b.X", INPUT1);
        String methodString = """
                String method(org.slf4j.Logger ext, Logger inner) {
                    return ext.getName() + inner.tag;
                }""";
        String out = isolate(X, "method", 2, methodString);
        @Language("java")
        String expected = """
                public class X_method {
                    class Logger { String tag; }
                    class org { class slf4j { class Logger {String getName() { return null; } } } }
                    String method(org.slf4j.Logger ext, Logger inner) {
                    return ext.getName() + inner.tag;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
