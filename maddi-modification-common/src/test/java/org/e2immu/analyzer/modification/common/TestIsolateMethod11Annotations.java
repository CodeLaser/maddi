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
                    @interface Named {String value() default ""; }
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

    @Language("java")
    public static final String A3 = """
            package a.b;
            public class X {
                String method(Named named) {
                    return named.value() + named.count();
                }
            }
            @interface Named { String value(); int count(); }
            """;

    /**
     * The point of this test is that an annotation attribute may not be stubbed like an ordinary method: the
     * ordinary shape is a body returning {@code null}, which yields an {@code @interface} that does not compile
     * and that the printer cannot even render. It used to check that by asserting the attribute stayed
     * <b>abstract</b>; an attribute now carries a {@code default} instead, which is not abstract and is just as
     * good against the thing being guarded against — the default is the value, not a return statement, and it is
     * there so that a use of the annotation which omits the attribute still compiles (a class isolate emitting a
     * JUnit 4 test class needs exactly that: one {@code @Test(expected=…)} and hundreds of bare {@code @Test}).
     */
    @DisplayName("an attribute read on an annotation instance is stubbed as an attribute, not as a method")
    @Test
    public void a3() {
        TypeInfo x = parse("a.b.X", A3);
        String m = """
                String method(Named named) {
                    return named.value() + named.count();
                }""";
        String out = isolate(x, "method", 1, m);
        @Language("java")
        String expected = """
                import java.lang.annotation.Annotation;
                public class X_method {
                    @interface Named {String value() default "";int count() default 0; }
                    String method(Named named) {
                    return named.value() + named.count();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
