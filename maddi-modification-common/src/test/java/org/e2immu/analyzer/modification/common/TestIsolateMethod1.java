package org.e2immu.analyzer.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.util.IsolateMethod;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestIsolateMethod1 {

    private JavaInspector javaInspector;
    IsolateMethod isolateMethod;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger(IsolateMethod.class)).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = javaInspectorFactory().withSources(SourceSetImpl.testProtocolSourceSet());
        isolateMethod = new IsolateMethod(javaInspector, "");
    }

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.util.ArrayList;
            import java.util.List;
            public class X {
                public List<String> method(List<String> input) {
                    if(input == null) {
                        return new ArrayList<>();
                    }
                    return input;
                }
            }
            """;

    @DisplayName("imports of java.util")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        IsolateMethod.Result r = isolateMethod.isolate(X.findUniqueMethod("method", 1));

        @Language("java")
        String expected = """
                import java.util.ArrayList;
                import java.util.List;
                public class X_method { }
                """;
        assertEquals(expected, isolateMethod.print(r));
    }


    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            import java.io.IOException;
            import java.util.ArrayList;
            import java.util.Arrays;
            import java.util.List;
            public class X {
                public List<String> method(List<String> input) {
                    if(length(input) < 2) {
                        List<String> newList = new ArrayList<>();
                        add(newList, "2", "3");
                        return newList;
                    }
                    return input;
                }
                static int length(List<String> list) { return list.size(); }
                void add(List<String> list, String... elements) {
                    Arrays.stream(elements).forEach(e -> list.add(e.toLowerCase()));
                }
            }
            """;

    @DisplayName("local method, import")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        IsolateMethod.Result r = isolateMethod.isolate(X.findUniqueMethod("method", 1));

        // import should not contain Arrays (only used outside 'method()'), IOException (not used)
        @Language("java")
        String expected = """
                import java.util.ArrayList;
                import java.util.List;
                public class X_method {
                    static int length(List<String> list) { return 0; }
                    void add(List<String> list, String ... elements) { }
                }
                """;
        assertEquals(expected, isolateMethod.print(r));
    }

    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import java.util.List;
            public class X {
                private static final Logger LOGGER = LoggerFactory.getLogger("X");
                record S(String s) { }
                record R(int i, List<S> strings) { }
                void method(R r) {
                    LOGGER.info("... {}", r);
                }
            }
            """;

    @DisplayName("local types, static field")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        IsolateMethod.Result r = isolateMethod.isolate(X.findUniqueMethod("method", 1));

        String methodString = """
                void method(R r) {
                    LOGGER.info("... {}", r);
                }""";
        @Language("java")
        String expected = """
                public class X_method {
                    Logger LOGGER;
                    class Logger {void info(String arg0, Object arg1) { } }
                    class R { }
                    void method(R r) {
                    LOGGER.info("... {}", r);
                }
                }
                """;
        assertEquals(expected, isolateMethod.print(r, methodString));
        javaInspector.invalidateAllSources();
        TypeInfo XMethod = javaInspector.parse("X_method", expected);
        assertNotNull(XMethod);
    }
}
