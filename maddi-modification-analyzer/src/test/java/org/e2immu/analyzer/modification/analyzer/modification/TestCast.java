package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.io.DecoratorImpl;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
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

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.DOWNCAST_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.SetOfTypeInfoImpl.EMPTY;
import static org.junit.jupiter.api.Assertions.*;

public class TestCast extends CommonTest {

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
        List<Info> ao = prepAnalyzer.doPrimaryType(X);
        analyzer.go(ao);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        ParameterInfo r = setAdd.parameters().getFirst();

        assertTrue(r.isModified());
        MethodLinkedVariables mlv = setAdd.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:r.object≥1:s,0:r.object.§$s∋1:s, 1:s∈0:r.object.§$s] --> -", mlv.toString());

        Value.VariableBooleanMap map = r.analysis().getOrDefault(PropertyImpl.MODIFIED_COMPONENTS_PARAMETER,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
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
        List<Info> ao = prepAnalyzer.doPrimaryType(X);
        analyzer.go(ao);

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
            Statement s0 = noCast.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viO0 = vd0.variableInfo("o");
            assertEquals("o←0:r.object", viO0.linkedVariables().toString());
        }

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        MethodLinkedVariables mlv = setAdd.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);

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
            assertEquals("set.§$s←0:r.object.§$s,set.§$s∋1:s,set←0:r.object", vi1Set.linkedVariables().toString());
            assertTrue(vi1Set.isModified());
            VariableInfo vi1R = vd1.variableInfo(r);
            assertTrue(vi1R.isModified());
        }
        assertTrue(r.isModified());
        assertEquals("[0:r.object≥1:s,0:r.object.§$s∋1:s, 1:s∈0:r.object.§$s] --> -", mlv.toString());
        Value.VariableBooleanMap map = r.analysis().getOrDefault(PropertyImpl.MODIFIED_COMPONENTS_PARAMETER,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
        assertEquals(1, map.map().size());
        assertEquals("{this.object=true}", map.map().toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            class B {
                void method(Object object) {
                    if(object instanceof Set<String> set) {
                        set.add("ok");
                    }
                }
            }
            """;

    @DisplayName("simple instanceof, downcast")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepAnalyzer.doPrimaryType(B);
        analyzer.go(ao);

        MethodInfo methodInfo = B.findUniqueMethod("method", 1);

        ParameterInfo pi0 = methodInfo.parameters().getFirst();
        Value.Independent objectIndependent = pi0.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT);
        assertTrue(objectIndependent.isIndependent());

        Statement s0If = methodInfo.methodBody().statements().getFirst();
        Statement s000Add = s0If.block().statements().getFirst();
        VariableData vd000 = VariableDataImpl.of(s000Add);
        VariableInfo vi000Set = vd000.variableInfo("set");
        assertTrue(vi000Set.isModified());
        VariableData vd0 = VariableDataImpl.of(s0If);

        VariableInfo vi0ObjectE = vd0.variableInfo(pi0, Stage.EVALUATION);
        Value.SetOfTypeInfo downcastsViE = vi0ObjectE.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE, EMPTY);
        assertEquals("[java.util.Set]", downcastsViE.typeInfoSet().toString());
        assertFalse(vi0ObjectE.isModified());

        VariableInfo vi0ObjectM = vd0.variableInfo(pi0, Stage.MERGE);
        Value.SetOfTypeInfo downcastsViM = vi0ObjectM.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE, EMPTY);
        assertEquals("[java.util.Set]", downcastsViM.typeInfoSet().toString());
        assertTrue(vi0ObjectM.isModified());

        Value.SetOfTypeInfo downcastsPi = pi0.analysis().getOrDefault(DOWNCAST_PARAMETER, EMPTY);
        assertEquals("[java.util.Set]", downcastsPi.typeInfoSet().toString());


        @Language("java")
        String expected = """
                package a.b;
                import java.util.Set;
                import org.e2immu.annotation.Immutable;
                import org.e2immu.annotation.Independent;
                import org.e2immu.annotation.Modified;
                import org.e2immu.annotation.NotModified;
                @Immutable(hc = true)
                @Independent
                class B {
                    @NotModified
                    void method(@Independent @Modified(downcast = true) Object object) {
                        if(object instanceof Set<String> set) { set.add("ok"); }
                    }
                }
                """;
        SourceSet sourceSetOfRequest = javaInspector.mainSources();
        // FIXME issue is "@Independent"
        assertEquals(expected, javaInspector.print2(B, new DecoratorImpl(runtime, sourceSetOfRequest),
                javaInspector.importComputer(4, sourceSetOfRequest)));
    }
}
