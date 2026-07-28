package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// exception types: a custom exception thrown/caught must be stubbed as a Throwable subtype so the text compiles
public class TestIsolateMethod7Exceptions extends CommonIsolateMethodTest {

    @Language("java")
    public static final String E1 = """
            package a.b;
            public class X {
                void method() throws MyException {
                    throw new MyException();
                }
            }
            class MyException extends Exception { }
            """;

    @DisplayName("custom checked exception in throws clause and a throw")
    @Test
    public void e1() {
        TypeInfo X = parse("a.b.X", E1);
        String m = """
                void method() throws MyException {
                    throw new MyException();
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class MyException extends Exception {MyException() { } }
                    void method() throws MyException {
                    throw new MyException();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String E2 = """
            package a.b;
            public class X {
                String method(Runnable r) {
                    try {
                        r.run();
                        return null;
                    } catch (MyException e) {
                        return e.detail;
                    }
                }
            }
            class MyException extends RuntimeException { String detail; }
            """;

    @DisplayName("custom exception caught in a catch clause (type only appears in the catch)")
    @Test
    public void e2() {
        TypeInfo X = parse("a.b.X", E2);
        String m = """
                String method(Runnable r) {
                    try {
                        r.run();
                        return null;
                    } catch (MyException e) {
                        return e.detail;
                    }
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class MyException extends RuntimeException { String detail; }
                    String method(Runnable r) {
                    try {
                        r.run();
                        return null;
                    } catch (MyException e) {
                        return e.detail;
                    }
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String E3 = """
            package a.b;
            import java.io.IOException;
            public class X {
                void helper() throws IOException { }
                void method() throws IOException {
                    helper();
                }
            }
            """;

    // The throws clause is the ONLY mention: the body calls a helper that declares it, so nothing the body
    // visitor sees names IOException. Before the fix in IsolateMethod.visit(Data, MethodInfo) the frame came
    // out without the import and the pasted signature did not resolve, which drops the whole unit on an
    // unresolved symbol. Found on closed-core (ExportJob.insertRecords, 'throws SQLException').
    @DisplayName("checked exception mentioned only in the throws clause")
    @Test
    public void e3() {
        TypeInfo X = parse("a.b.X", E3);
        String m = """
                void method() throws IOException {
                    helper();
                }""";
        String out = isolate(X, "method", 0, m);
        assertTrue(out.contains("import java.io.IOException;"),
                "the throws type must be imported, otherwise the frame does not resolve:\n" + out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String E4 = """
            package a.b;
            import java.util.EmptyStackException;
            public class X {
                String method(java.util.Stack<String> stack) {
                    try {
                        return stack.pop();
                    } catch (EmptyStackException e) {
                        return null;
                    }
                }
            }
            """;

    /**
     * A type named ONLY in a catch clause: the body mentions the variable, never the type, so nothing else in
     * the traversal reaches it and the frame was left without the import. Same shape as the throws-clause case
     * of {@link #e3()}, and it cost 21 of the 37 units still failing on the hundred-class corpus of IsolateClass.
     */
    @DisplayName("an exception named only in a catch clause is still imported")
    @Test
    public void e4() {
        TypeInfo x = parse("a.b.X", E4);
        String m = """
                String method(java.util.Stack<String> stack) {
                    try {
                        return stack.pop();
                    } catch (EmptyStackException e) {
                        return null;
                    }
                }""";
        String out = isolate(x, "method", 1, m);
        assertTrue(out.contains("import java.util.EmptyStackException;"), out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
