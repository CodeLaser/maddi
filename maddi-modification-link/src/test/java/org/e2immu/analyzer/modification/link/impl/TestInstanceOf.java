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
        ParameterInfo s = setAdd.parameters().getLast();
        {
            Statement s0 = setAdd.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viObject0E = vd0.variableInfo(object, Stage.EVALUATION);
            assertEquals("0:object→set", viObject0E.linkedVariables().toString());
            assertFalse(viObject0E.isModified());
        }
        {
            Statement s000 = setAdd.methodBody().statements().getFirst().block().statements().getFirst();
            VariableData vd000 = VariableDataImpl.of(s000);
            VariableInfo viS000 = vd000.variableInfo(s);
            assertEquals("1:s∈0:object.§es,1:s∈set.§es", viS000.linkedVariables().toString());
            assertFalse(viS000.isModified());
            VariableInfo viObject000 = vd000.variableInfo(object);
            assertEquals("0:object.§es→set.§es,0:object.§es∋1:s,0:object.§m≡set.§m,0:object→set",
                    viObject000.linkedVariables().toString());
            assertTrue(viObject000.isModified());
        }
        assertTrue(object.isModified());
        assertEquals("[0:object*.§es∋1:s, 1:s∈0:object*.§es] --> -", mlv.toString());
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

        Statement s0 = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);

        VariableInfo viI0E = vd0.variableInfo(i, Stage.EVALUATION);
        assertEquals("0:i≻o,0:i≻set", viI0E.linkedVariables().toString());
        assertFalse(viI0E.isModified());
        assertEquals("[a.b.X.R]", viI0E.downcast().toString());

        VariableInfo viSet0E = vd0.variableInfo("set", Stage.EVALUATION);
        assertEquals("set←o,set≺0:i", viSet0E.linkedVariables().toString());
        assertFalse(viI0E.isModified());

        VariableInfo viO0E = vd0.variableInfo("o", Stage.EVALUATION);
        assertEquals("[java.util.Set]", viO0E.downcast().toString());

        Statement s000 = method.methodBody().statements().getFirst().block().statements().getFirst();
        VariableData vd000 = VariableDataImpl.of(s000);

        VariableInfo vi000Set = vd000.variableInfo("set");
        assertTrue(vi000Set.isModified());
        assertEquals("set.§es←o.§es,set.§es∋1:s,set.§es≺0:i,set.§m≡o.§m,set←o",
                vi000Set.linkedVariables().toString());

        VariableInfo vi000O = vd000.variableInfo("o");
        assertEquals("o.§es→set.§es,o.§es∋1:s,o.§m≡set.§m,o→set,o∩0:i", vi000O.linkedVariables().toString());
        // o ≺ 0:i is not visible
        assertTrue(vi000O.isModified());

        VariableInfo vi000I = vd000.variableInfo(i);
        assertEquals("0:i≥1:s,0:i∩o.§es,0:i∩set.§es,0:i≈o,0:i≈set", vi000I.linkedVariables().toString());
        // o ≺ 0:i is not visible
        assertTrue(vi000I.isModified());
        assertEquals("[a.b.X.R]", vi000I.downcast().toString());

        VariableInfo viI0M = vd0.variableInfo(i, Stage.MERGE);
        assertEquals("0:i≻o,0:i≻set,0:i≥1:s,0:i∩o.§es,0:i∩set.§es", viI0M.linkedVariables().toString());
        assertTrue(viI0M.isModified());
        assertEquals("[a.b.X.R]", viI0M.downcast().toString());

        assertTrue(i.isModified());
        assertEquals("[0:i*≥1:s, 1:s≤0:i*] --> -", mlv.toString());
        // NOTE: there are no modified components, they are hidden by the downcast
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            class X {
                final static class M {
                    private int i;
                    public int getI() { return i; }
                    public void setI(int i) { this.i = i; }
                }
                static M method(Object object) {
                    if (object instanceof M m) {
                        return m;
                    }
                    return null;
                }
            }
            """;

    @DisplayName("instanceof pattern variable")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement s000 = method.methodBody().statements().getFirst().block().statements().getFirst();
        VariableData vd000 = VariableDataImpl.of(s000);
        VariableInfo rv000 = vd000.variableInfo(method.fullyQualifiedName());
        assertEquals("method←0:object", rv000.linkedVariables().toString());

        Statement s0 = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo rv0 = vd0.variableInfo(method.fullyQualifiedName());
        assertEquals("method←0:object", rv0.linkedVariables().toString());

        Statement s1 = method.methodBody().statements().getFirst();
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo rv1 = vd1.variableInfo(method.fullyQualifiedName());
        assertEquals("method←0:object", rv1.linkedVariables().toString());

        VariableData vd = VariableDataImpl.of(method);
        VariableInfo rv = vd.variableInfo(method.fullyQualifiedName());
        assertEquals("method←$_ce0,method←0:object", rv.linkedVariables().toString());

        assertEquals("[-] --> method←$_ce0,method←0:object", mlv.toString());
        //the $_ce0 informs us that this in not an @Identity method!
    }
}
