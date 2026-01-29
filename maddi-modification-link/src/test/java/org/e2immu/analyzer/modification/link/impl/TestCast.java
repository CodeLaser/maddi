package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
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

public class TestCast extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                static boolean setAdd(Object object, String s) {
                    Set<String> set = (Set<String>) object;
                    return set.add(s);
                }
            }
            """;

    @DisplayName("links of cast")
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
            VariableInfo viObject0 = vd0.variableInfo(object);
            assertEquals("0:object→set,0:object.§m≡set.§m", viObject0.linkedVariables().toString());
            assertFalse(viObject0.isModified());
        }
        {
            Statement s1 = setAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo viObject1 = vd1.variableInfo(object);
            assertEquals("0:object.§$s→set.§$s,0:object.§$s∋1:s,0:object.§m≡set.§m,0:object→set",
                    viObject1.linkedVariables().toString());
            assertTrue(viObject1.isModified());
        }
        assertTrue(object.isModified());
        assertEquals("[0:object*.§$s∋1:s, 1:s∈0:object*.§$s] --> -", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Object object) {}
                static boolean setAdd(R r, String s) {
                    Set<String> set = (Set<String>) r.object;
                    return set.add(s);
                }
            }
            """;

    @DisplayName("links of cast in record")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        MethodLinkedVariables mlv = setAdd.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setAdd));

        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo r = setAdd.parameters().getFirst();
        {
            Statement s0 = setAdd.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0Set = vd0.variableInfo("set");
            assertEquals("set←0:r.object", vi0Set.linkedVariables().toString());
            assertFalse(vi0Set.isModified());
        }
        {
            Statement s1 = setAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Set = vd1.variableInfo("set");
            assertEquals("set.§$s←0:r.object.§$s,set.§$s∋1:s,set.§m≡0:r.object.§m,set←0:r.object",
                    vi1Set.linkedVariables().toString());
            assertTrue(vi1Set.isModified());
            VariableInfo vi1R = vd1.variableInfo(r);
            assertTrue(vi1R.isModified());
        }
        assertTrue(r.isModified());
        // old version of Util.isPartOf():[0:r.object≥1:s,0:r.object.§$s∋1:s, 1:s∈0:r.object.§$s] --> -
        assertEquals("[0:r.object*.§$s∋1:s, 1:s∈0:r.object*.§$s] --> -", mlv.toString());
    }
}
