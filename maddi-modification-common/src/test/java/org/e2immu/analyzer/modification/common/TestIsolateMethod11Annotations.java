package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// annotations on the isolated method: the annotation type (and any attributes used) must be stubbed
public class TestIsolateMethod11Annotations extends CommonIsolateMethodTest {

    @Language("java")
    public static final String A1 = """
            package a.b;
            public class X {
                @Marker
                String method() {
                    return "x";
                }
            }
            @interface Marker { }
            """;

    @DisplayName("marker annotation on the method")
    @Test
    public void a1() {
        TypeInfo x = parse("a.b.X", A1);
        String m = """
                @Marker
                String method() {
                    return "x";
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                import java.lang.annotation.Annotation;
                public class X_method {
                    @interface Marker { }
                    @Marker
                String method() {
                    return "x";
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String A2 = """
            package a.b;
            public class X {
                @Named("orderService")
                String method() {
                    return "x";
                }
            }
            @interface Named { String value(); }
            """;

    @DisplayName("annotation with a value attribute on the method")
    @Test
    public void a2() {
        TypeInfo x = parse("a.b.X", A2);
        String m = """
                @Named("orderService")
                String method() {
                    return "x";
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                import java.lang.annotation.Annotation;
                public class X_method {
                    @interface Named {String value(); }
                    @Named("orderService")
                String method() {
                    return "x";
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
