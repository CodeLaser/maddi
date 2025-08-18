package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestCast extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                interface Exit {}
                record ExceptionThrown(Exception exception) implements Exit {
                }
                interface Interface {
                    Exception exception();
                }
                static class I implements Interface {
                    Exit exit;
            
                    @Override
                    public Exception exception() {
                        if (exit instanceof ExceptionThrown) {
                            ExceptionThrown et = ((ExceptionThrown) exit);
                            return et.exception();
                        }
                        return null;
                    }
                }
            }
            """;


    @DisplayName("field of getter should exist, analysis order")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        List<Info> analysisOrder = analyzer.doPrimaryType(X);
        // Exit should come before ExceptionThrown
        assertEquals("""
               a.b.X.<init>(), a.b.X.ExceptionThrown.<init>(Exception), a.b.X.ExceptionThrown.exception(), \
               a.b.X.Exit, a.b.X.I.<init>(), a.b.X.Interface.exception(), a.b.X.ExceptionThrown.exception, \
               a.b.X.Interface, a.b.X.ExceptionThrown, a.b.X.I.exception(), a.b.X.I.exit, a.b.X.I, a.b.X\
               """, analysisOrder.stream().map(Info::fullyQualifiedName).collect(Collectors.joining(", ")));

        TypeInfo exceptionThrown = X.findSubType("ExceptionThrown");
        MethodInfo exceptionAccessor = exceptionThrown.findUniqueMethod("exception", 0);
        assertNotNull(exceptionAccessor.getSetField().field());

        TypeInfo implementation = X.findSubType("I");
        MethodInfo exception = implementation.findUniqueMethod("exception", 0);
        Statement s001 = exception.methodBody().statements().getFirst().block().statements().get(1);
        VariableData vd001 = VariableDataImpl.of(s001);
        assertEquals("a.b.X.ExceptionThrown.exception#et, a.b.X.I.exception(), a.b.X.I.exit, a.b.X.I.this, et",
                vd001.knownVariableNamesToString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X {
                interface Exit {}
                record ExceptionThrown(Exception exception) implements Exit {
                }
                interface Interface {
                    Exception exception();
                }
                static class I implements Interface {
                    Exit exit;
            
                    @Override
                    public Exception exception() {
                        if (exit instanceof ExceptionThrown) return ((ExceptionThrown) exit).exception();
                        return null;
                    }
                }
            }
            """;


    @DisplayName("field of getter should exist, in one statement")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);
        TypeInfo exceptionThrown = X.findSubType("ExceptionThrown");
        MethodInfo exceptionAccessor = exceptionThrown.findUniqueMethod("exception", 0);
        assertNotNull(exceptionAccessor.getSetField().field());

        TypeInfo implementation = X.findSubType("I");
        MethodInfo exception = implementation.findUniqueMethod("exception", 0);
        Statement s000 = exception.methodBody().statements().get(0).block().statements().get(0);
        VariableData vd000 = VariableDataImpl.of(s000);
        assertEquals("a.b.X.ExceptionThrown.exception#a.b.X.I.exit, a.b.X.I.exception(), a.b.X.I.exit, a.b.X.I.this",
                vd000.knownVariableNamesToString());
    }
}
