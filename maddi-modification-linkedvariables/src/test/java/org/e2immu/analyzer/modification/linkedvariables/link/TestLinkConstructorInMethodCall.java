package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER;
import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.STATIC_VALUES_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.NO_VALUE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkConstructorInMethodCall extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                interface Exit { }
            
                record ExceptionThrown(Exception exception) implements Exit { }
            
                interface LoopData {
                    LoopData withException(Exception e);
                }
            
                static class LoopDataImpl implements LoopData {
                    private Exit exit;
            
                    LoopDataImpl(Exit exit) {
                        this.exit = exit;
                    }
            
                    @Override
                    public LoopData withException(Exception e) {
                        Exit ee = new ExceptionThrown(e);
                        return new LoopDataImpl(ee);
                    }
                }
            }
            """;

    @DisplayName("construction separate from method call")
    @Test
    public void test1() {
        // see TestAnalysisOrder,2 for a test of the analysis order of this code
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.ExceptionThrown.<init>(Exception), a.b.X.ExceptionThrown.exception(), \
                a.b.X.Exit, a.b.X.LoopData.withException(Exception), a.b.X.ExceptionThrown.exception, a.b.X.LoopData, \
                a.b.X.LoopDataImpl.<init>(a.b.X.Exit), a.b.X.ExceptionThrown, a.b.X.LoopDataImpl.exit, \
                a.b.X.LoopDataImpl.withException(Exception), a.b.X.LoopDataImpl, a.b.X]\
                """, analysisOrder.toString());

        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        FieldInfo exit = loopDataImpl.getFieldByName("exit", true);
        assertTrue(exit.isPropertyFinal());

        analyzer.go(analysisOrder, 2);

        testImmutable(X);

        TypeInfo exceptionThrown = X.findSubType("ExceptionThrown");
        {
            HiddenContentTypes exceptionThrownHct = exceptionThrown.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
            assertEquals("0=Exception", exceptionThrownHct.detailedSortedTypes());
            MethodInfo exceptionAccessor = exceptionThrown.findUniqueMethod("exception", 0);
            HiddenContentTypes exceptionAccessorHct = exceptionAccessor.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
            assertEquals("0=Exception - ", exceptionAccessorHct.detailedSortedTypes());

            FieldInfo exception = exceptionThrown.getFieldByName("exception", true);
            assertTrue(exception.isPropertyFinal());

            assertTrue(exceptionThrown.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isMutable());
            assertSame(FINAL_FIELDS, exceptionThrown.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        }

        {
            HiddenContentTypes ldiHct = loopDataImpl.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
            assertEquals("0=Exit", ldiHct.detailedSortedTypes());

            {
                MethodInfo constructor = loopDataImpl.findConstructor(1);
                ParameterInfo p0 = constructor.parameters().getFirst();
                assertEquals("E=this.exit", p0.analysis().getOrDefault(STATIC_VALUES_PARAMETER,
                        StaticValuesImpl.NONE).toString());
                assertEquals("-1-:exit", p0.analysis().getOrDefault(LINKED_VARIABLES_PARAMETER,
                        LinkedVariablesImpl.EMPTY).toString());
                assertTrue(p0.analysis().getOrDefault(MODIFIED_COMPONENTS_PARAMETER,
                        ValueImpl.VariableBooleanMapImpl.EMPTY).isEmpty());
                assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
            }
            MethodInfo withException = loopDataImpl.findUniqueMethod("withException", 1);
            assertTrue(withException.isNonModifying());
            assertSame(INDEPENDENT, withException.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

            {
                Statement s0 = withException.methodBody().statements().getFirst();
                VariableData vd0 = VariableDataImpl.of(s0);
                VariableInfo vi0Ee = vd0.variableInfo("ee");
                assertEquals("Type a.b.X.ExceptionThrown E=new ExceptionThrown(e) this.exception=e", vi0Ee.staticValues().toString());
                // the "M" is there because the hidden content type "Exit" gets a concrete modifiable instance, ExceptionThrown
                // the -2- is there because the objects are of different types (ExceptionThrown vs Exception)
                // the "F" and "*" indicate that a field in ET is linked to the whole Exception object
                assertEquals("0M-2-*M|0-*:e", vi0Ee.linkedVariables().toString());
            }
            {
                Statement s1 = withException.methodBody().lastStatement();
                VariableData vd1 = VariableDataImpl.of(s1);
                VariableInfo vi1Rv = vd1.variableInfo(withException.fullyQualifiedName());
                assertEquals("Type a.b.X.LoopDataImpl E=new LoopDataImpl(ee) this.exit=ee", vi1Rv.staticValues().toString());
                assertEquals("0M-4-*M:e, 0M-4-*M:ee", vi1Rv.linkedVariables().toString());
                // modification areas missing because 4-links: "0M-4-*M|0.0-*:e, 0M-4-*M|0-*:ee"
            }
        }
        testLoopData(X);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                interface Exit { }
            
                record ExceptionThrown(Exception exception) implements Exit { }
            
                interface LoopData {
                    LoopData withException(Exception e);
                }
            
                static class LoopDataImpl implements LoopData {
                    private final Exit exit;
            
                    LoopDataImpl(Exit exit) {
                        this.exit = exit;
                    }
            
                    @Override
                    public LoopData withException(Exception e) {
                        return new LoopDataImpl(new ExceptionThrown(e)); // sole difference is here
                    }
                }
            }
            """;

    @DisplayName("construction as argument to method call")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder, 2);
        testImmutable(X);

        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        MethodInfo ldConstructor = loopDataImpl.findConstructor(1);
        ParameterInfo ldConstructor0 = ldConstructor.parameters().getFirst();
        StaticValues sv0 = ldConstructor0.analysis().getOrDefault(STATIC_VALUES_PARAMETER, StaticValuesImpl.NONE);
        assertEquals("E=this.exit", sv0.toString());

        MethodInfo withException = loopDataImpl.findUniqueMethod("withException", 1);
        assertTrue(withException.isNonModifying());
        {
            Statement s0 = withException.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0Rv = vd0.variableInfo(withException.fullyQualifiedName());
            // formal type of variable:
            assertEquals("Type a.b.X.LoopData", vi0Rv.variable().parameterizedType().toString());
            assertEquals("""
                    Type a.b.X.LoopDataImpl E=new LoopDataImpl(new ExceptionThrown(e)) this.exit=new ExceptionThrown(e)\
                    """, vi0Rv.staticValues().toString());
            assertEquals("0M-4-*M:e", vi0Rv.linkedVariables().toString()); // |0.0-*:e is missing, because 4-link
        }
        testLoopData(X);
    }

    private void testImmutable(TypeInfo X) {
        TypeInfo exception = javaInspector.compiledTypesManager().get(Exception.class);
        assertTrue(exception.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isMutable());

        TypeInfo exit = X.findSubType("Exit");
        assertSame(IMMUTABLE_HC, exit.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
    }

    private void testLoopData(TypeInfo X) {
        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        MethodInfo ldImplWithException = loopDataImpl.findUniqueMethod("withException", 1);
        assertTrue(ldImplWithException.isNonModifying());
        assertSame(INDEPENDENT_HC, loopDataImpl.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(IMMUTABLE_HC, loopDataImpl.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));

        // LoopData's properties are computed from LoopDataImpl
        TypeInfo loopData = X.findSubType("LoopData");
        MethodInfo ldWithException = loopData.findUniqueMethod("withException", 1);
        assertFalse(ldWithException.isModifying());
        assertSame(INDEPENDENT, ldWithException.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD, INDEPENDENT_HC));
        assertSame(IMMUTABLE_HC, loopData.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
    }
}
