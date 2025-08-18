package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.WhileStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAssignmentsInstanceOf extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            import java.io.IOException;
            public class X {
                public static void method(Exception exception) {
                    if (exception instanceof RuntimeException e) {
                        throw e;
                    } else {
                        String e = exception.getMessage();
                        System.out.println("e = " + e.toLowerCase());
                    }
                }
            }
            """;

    @DisplayName("positive instanceof")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);
        IfElseStatement ifElse = (IfElseStatement) method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(ifElse);
        assertEquals("X.method(Exception), X.method(Exception):0:exception, e, java.lang.System.out",
                vd0.knownVariableNamesToString());
        VariableInfo vi0 = vd0.variableInfo("e");
        assertEquals("D:0-E, A:[0-E]", vi0.assignments().toString());
        assertEquals("Type RuntimeException", vi0.variable().parameterizedType().toString());

        VariableData vd000 = VariableDataImpl.of(ifElse.block().statements().get(0));
        assertEquals("X.method(Exception), X.method(Exception):0:exception, e",
                vd000.knownVariableNamesToString());
        VariableInfo vi000 = vd0.variableInfo("e");
        assertEquals("D:0-E, A:[0-E]", vi000.assignments().toString());
        assertEquals("Type RuntimeException", vi000.variable().parameterizedType().toString());

        VariableData vd010 = VariableDataImpl.of(ifElse.elseBlock().statements().get(0));
        assertEquals("X.method(Exception):0:exception, e", vd010.knownVariableNamesToString());
        VariableInfo vi010 = vd010.variableInfo("e");
        assertEquals("D:0.1.0, A:[0.1.0]", vi010.assignments().toString());
        assertEquals("Type String", vi010.variable().parameterizedType().toString());

        VariableData vd011 = VariableDataImpl.of(ifElse.elseBlock().statements().get(1));
        assertEquals("X.method(Exception):0:exception, e, java.lang.System.out", vd011.knownVariableNamesToString());
        VariableInfo vi011 = vd011.variableInfo("e");
        assertEquals("D:0.1.0, A:[0.1.0]", vi011.assignments().toString());
        assertEquals("Type String", vi011.variable().parameterizedType().toString());
    }

    @Language("java")
    public static final String INPUT2 = """
            import java.io.IOException;
            public class X {
                public static void method(Exception exception) {
                    if (!(exception instanceof RuntimeException e)) {
                        String e = exception.getMessage();
                        System.out.println("e = " + e.toLowerCase());
                    } else {
                        throw e;
                    }
                }
            }
            """;

    @DisplayName("negative instanceof")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);
        IfElseStatement ifElse = (IfElseStatement) method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(ifElse);
        assertEquals("X.method(Exception), X.method(Exception):0:exception, e, java.lang.System.out",
                vd0.knownVariableNamesToString());
        VariableInfo vi0 = vd0.variableInfo("e");
        assertEquals("D:0-E, A:[0-E]", vi0.assignments().toString());
        assertEquals("Type RuntimeException", vi0.variable().parameterizedType().toString());

        VariableData vd010 = VariableDataImpl.of(ifElse.elseBlock().statements().get(0));
        assertEquals("X.method(Exception), X.method(Exception):0:exception, e",
                vd010.knownVariableNamesToString());
        VariableInfo vi010 = vd0.variableInfo("e");
        assertEquals("D:0-E, A:[0-E]", vi010.assignments().toString());
        assertEquals("Type RuntimeException", vi010.variable().parameterizedType().toString());

        VariableData vd001 = VariableDataImpl.of(ifElse.block().statements().get(1));
        assertEquals("X.method(Exception):0:exception, e, java.lang.System.out", vd001.knownVariableNamesToString());
        VariableInfo vi001 = vd001.variableInfo("e");
        assertEquals("D:0.0.0, A:[0.0.0]", vi001.assignments().toString());
        assertEquals("Type String", vi001.variable().parameterizedType().toString());
    }


    @Language("java")
    public static final String INPUT3 = """
            import java.io.IOException;
            public class X {
                interface T { }
                static class MyException extends RuntimeException {
                    T t;
                    T getT() { return t; }
                    void setT(T t) { this.t = t; }
                }
                static void method(Exception exception, T tt1, T tt2) {
                    if (exception instanceof MyException my1 && tt2.equals(my1.getT())) {
                        System.out.println(my1.getMessage());
                    } else {
                        System.out.println("?");
                    }
                }
            }
            """;

    @DisplayName("instanceof, negative part")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 3);
        IfElseStatement ifElse = (IfElseStatement) method.methodBody().statements().getFirst();
        Statement statement = ifElse.elseBlock().statements().getFirst();
        VariableData vdWhile = VariableDataImpl.of(statement);
        // we should not see anything related to my1 here!! not my1, not my1.t ~ my1.getT()
        // neither should tt1 and tt2 be present, they are not used here
        assertEquals("""
                X.method(Exception,X.T,X.T):0:exception, X.method(Exception,X.T,X.T):2:tt2, java.lang.System.out\
                """, vdWhile.knownVariableNamesToString());
    }


    @Language("java")
    public static final String INPUT3b = """
            import java.io.IOException;
            public class X {
                interface T { }
                static class MyException extends RuntimeException {
                    T t;
                    T getT() { return t; }
                    void setT(T t) { this.t = t; }
                }
                static void method(Exception exception, T tt1, T tt2) {
                    if (exception instanceof MyException my && tt2.equals(my.getT())) {
                        System.out.println(my.getMessage());
                    } else {
                        while (exception instanceof RuntimeException e) {
                            String msg = e.getMessage();
                            System.out.println("e = " + msg.toLowerCase());
                            exception = (Exception) e.getCause();
                            if (e.getCause() instanceof MyException my && tt1.equals(my.getT())) {
                                break;
                            }
                        }
                    }
                }
            }
            """;

    // this is the full version of test3; originally caught as a problem due to re-use of "my"
    @DisplayName("instanceof, negative part; re-use variable name")
    @Test
    public void test3b() {
        TypeInfo X = javaInspector.parse(INPUT3b);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);
    }
}