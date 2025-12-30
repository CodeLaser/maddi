package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestInstanceOf extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                static boolean setAdd(Object object, String s) {
                    if(object instanceof Set set) {
                        return set.add(s);
                    }
                    return false;
                }
            }
            """;

    @DisplayName("links of instanceof")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        MethodLinkedVariables mlv = setAdd.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setAdd));

        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo object = setAdd.parameters().getFirst();
        {
            Statement s0 = setAdd.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viObject0E = vd0.variableInfo(object, Stage.EVALUATION);
            assertEquals("0:object→set", viObject0E.linkedVariables().toString());
            assertFalse(viObject0E.isModified());
        }
        {
            Statement s000 = setAdd.methodBody().statements().getFirst();
            VariableData vd000 = VariableDataImpl.of(s000);
            VariableInfo viObject000 = vd000.variableInfo(object);
            assertEquals("0:object→set,0:object.§es→set.§es,0:object.§es∋1:s,0:object→set", viObject000.linkedVariables().toString());
            assertTrue(viObject000.isModified());
        }
        assertTrue(object.isModified());
        assertEquals("[0:object.§es∋1:s, 1:s∈0:object.§es] --> -", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                interface I { }
                record R(Object o) implements I {}
                void method(I i, String s) {
                    if(i instanceof R(Object o) && o instanceof Set set) {
                        set.add(s);
                    }
                }
            }
            """;

    @DisplayName("links of instanceof with record pattern")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd = VariableDataImpl.of(method);
        assertNotNull(vd);
        ParameterInfo i = method.parameters().getFirst();
        {
            Statement s000 = method.methodBody().statements().getFirst().block().statements().getFirst();
            VariableData vd000 = VariableDataImpl.of(s000);
            VariableInfo vi000O = vd000.variableInfo("o");
            assertEquals("o.§es→set.§es,o.§es∋1:s,o.§es≺0:i,o→set", vi000O.linkedVariables().toString());
            // o ≺ 0:i is not visible
            assertTrue(vi000O.isModified());

            VariableInfo vi000I = vd000.variableInfo(i);
            assertEquals("0:i≥1:s,0:i∩o.§es,0:i∩set.§es,0:i≈o,0:i≈set", vi000I.linkedVariables().toString());
            // o ≺ 0:i is not visible
            assertTrue(vi000I.isModified());
        }
        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viI0E = vd0.variableInfo(i, Stage.EVALUATION);
            assertEquals("0:i≻o,0:i≻set", viI0E.linkedVariables().toString());
            assertFalse(viI0E.isModified());

            VariableInfo viI0M = vd0.variableInfo(i, Stage.MERGE);
            assertEquals("0:i≻o,0:i≥1:s,0:i∩o.§es,0:i∩set.§es,0:i≈o,0:i≈set", viI0M.linkedVariables().toString());
            assertTrue(viI0M.isModified());
        }

        assertTrue(i.isModified());
        assertEquals("[0:i≥1:s, 1:s≤0:i] --> -", mlv.toString());
    }

}
