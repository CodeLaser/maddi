package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLinkCast extends CommonTest {

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
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo object = setAdd.parameters().get(0);
        {
            Statement s0 = setAdd.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viObject0 = vd0.variableInfo(object);
            assertEquals("-1-:set", viObject0.linkedVariables().toString());
            assertFalse(viObject0.isModified());
        }
        {
            Statement s1 = setAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo viObject1 = vd1.variableInfo(object);
            assertEquals("-1-:set", viObject1.linkedVariables().toString());
            assertTrue(viObject1.isModified());
        }
        assertTrue(object.isModified());
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
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo r = setAdd.parameters().get(0);
        {
            Statement s0 = setAdd.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0Set = vd0.variableInfo("set");
            assertEquals("-1-:object, *M-2-0M|*-0:r", vi0Set.linkedVariables().toString());
            assertFalse(vi0Set.isModified());
        }
        {
            Statement s1 = setAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Set = vd1.variableInfo("set");
            assertEquals("-1-:object, *M-2-0M|*-0:r", vi1Set.linkedVariables().toString());
            assertTrue(vi1Set.isModified());
            VariableInfo vi1R = vd1.variableInfo(r);
            assertTrue(vi1R.isModified());
        }
        assertTrue(r.isModified());
        Value.VariableBooleanMap map = r.analysis().getOrDefault(PropertyImpl.MODIFIED_COMPONENTS_PARAMETER, ValueImpl.VariableBooleanMapImpl.EMPTY);
        assertEquals(1, map.map().size());
        assertEquals("{this.object=true}", map.map().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Object object, int i) {}
                static boolean noCast(R r) {
                    Object o = r.object();
                    return o != null;
                }
                static boolean setAdd(R r, String s) {
                    Set<String> set = (Set<String>) r.object();
                    return set.add(s);
                }
            }
            """;

    @DisplayName("links of cast in record, accessor")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        // first, test independence of accessors

        TypeInfo R = X.findSubType("R");
        MethodInfo iAccessor = R.findUniqueMethod("i", 0);
        assertSame(R.getFieldByName("i", true), iAccessor.getSetField().field());
        Value.Independent iaIndependent = iAccessor.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                ValueImpl.IndependentImpl.DEPENDENT);
        assertTrue(iaIndependent.isIndependent());

        MethodInfo objectAccessor = R.findUniqueMethod("object", 0);
        assertSame(R.getFieldByName("object", true), objectAccessor.getSetField().field());
        Value.Independent oaIndependent = objectAccessor.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                ValueImpl.IndependentImpl.DEPENDENT);
        assertTrue(oaIndependent.isIndependentHc());

        MethodInfo noCast = X.findUniqueMethod("noCast", 1);
        {
            Statement s0 = noCast.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viO0 = vd0.variableInfo("o");
            assertEquals("-1-:object, *-4-0:r", viO0.linkedVariables().toString());
            assertEquals("E=r.object", viO0.staticValues().toString());
        }
        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo r = setAdd.parameters().get(0);
        {
            Statement s0 = setAdd.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0Set = vd0.variableInfo("set");
            assertEquals("-1-:object, *M-2-0M|*-0:r", vi0Set.linkedVariables().toString());
            assertEquals("E=r.object", vi0Set.staticValues().toString());
            assertFalse(vi0Set.isModified());
        }
        {
            Statement s1 = setAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Set = vd1.variableInfo("set");
            assertEquals("-1-:object, *M-2-0M|*-0:r", vi1Set.linkedVariables().toString());
            assertEquals("E=r.object", vi1Set.staticValues().toString());
            assertTrue(vi1Set.isModified());
            VariableInfo vi1R = vd1.variableInfo(r);
            assertTrue(vi1R.isModified());
        }
        assertTrue(r.isModified());
        Value.VariableBooleanMap map = r.analysis().getOrDefault(PropertyImpl.MODIFIED_COMPONENTS_PARAMETER,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
        assertEquals(1, map.map().size());
        assertEquals("{this.object=true}", map.map().toString());
    }

}
