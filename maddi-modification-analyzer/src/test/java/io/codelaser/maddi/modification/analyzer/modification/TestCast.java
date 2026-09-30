package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl;
import io.codelaser.maddi.modification.prepwork.io.DecoratorImpl;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.Stage;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableInfoImpl;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.DOWNCAST_PARAMETER;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.SetOfTypeInfoImpl.EMPTY;
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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        List<Info> ao = prepAnalyzer.doPrimaryType(X);
        analyzer.go(ao);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        ParameterInfo r = setAdd.parameters().getFirst();

        assertTrue(r.isModified());
        MethodLinkedVariables mlv = setAdd.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:r.object*.§$s∋1:s, 1:s≤0:r*,1:s∈0:r.object*.§$s] --> -", mlv.toString());
        assertEquals("a.b.X.setAdd(a.b.X.R,String):0:r, r.object", mlv.sortedModifiedString());
        assertEquals("r.object->[java.util.Set]", r.analysis().getOrNull(DOWNCAST_PARAMETER,
                ValueImpl.VariableToTypeInfoSetImpl.class).nice());
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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
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
            assertEquals("set.§m≡0:r.object.§m,set←0:r.object", vi0Set.linkedVariables().toString());
            assertFalse(vi0Set.isModified());
            assertEquals("""
                    a.b.X.R.object#a.b.X.setAdd(a.b.X.R,String):0:r, a.b.X.setAdd(a.b.X.R,String):0:r, set\
                    """, vd0.knownVariableNamesToString());
            VariableInfo rObject = vd0.variableInfo("a.b.X.R.object#a.b.X.setAdd(a.b.X.R,String):0:r");
            assertEquals("[java.util.Set]", rObject.downcast().toString());
        }
        {
            Statement s1 = setAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Set = vd1.variableInfo("set");
            assertEquals("set.§$s≺0:r,set.§$s∋1:s,set.§$s←0:r.object.§$s,set.§m≡0:r.object.§m,set←0:r.object",
                    vi1Set.linkedVariables().toString());
            assertTrue(vi1Set.isModified());
            VariableInfo vi1R = vd1.variableInfo(r);
            assertTrue(vi1R.isModified());
        }
        assertTrue(r.isModified());
        assertEquals("[0:r.object*.§$s∋1:s, 1:s≤0:r*,1:s∈0:r.object*.§$s] --> -", mlv.toString());
        assertEquals("a.b.X.setAdd(a.b.X.R,String):0:r, r.object", mlv.sortedModifiedString());
        assertEquals("r.object->[java.util.Set]", r.analysis().getOrNull(DOWNCAST_PARAMETER,
                ValueImpl.VariableToTypeInfoSetImpl.class).nice());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            class B {
                void method(Object object) {
                    if(object instanceof Set set) {
                        set.add("ok");
                    }
                }
            }
            """;

    @DisplayName("simple instanceof, downcast")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse("a.b.B", INPUT4);
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

        Value.VariableToTypeInfoSet downcastsPi = pi0.analysis().getOrDefault(DOWNCAST_PARAMETER, ValueImpl.VariableToTypeInfoSetImpl.EMPTY);
        assertEquals("{a.b.B.method(Object):0:object=[java.util.Set]}", downcastsPi.variableToTypeInfoSet().toString());
        assertTrue(pi0.isModified());

        @Language("java")
        String expected = """
                package a.b;
                import java.util.Set;
                import io.codelaser.maddi.annotation.Immutable;
                import io.codelaser.maddi.annotation.Independent;
                import io.codelaser.maddi.annotation.Modified;
                import io.codelaser.maddi.annotation.NotModified;
                @Immutable(hc = true)
                @Independent
                class B {
                    @NotModified
                    void method(@Independent @Modified(downcast = true, downcastTo = { "java.util.Set" }) Object object) {
                        if (object instanceof Set set) { set.add("ok"); }
                    }
                }
                """;
        SourceSet sourceSetOfRequest = javaInspector.mainSources();
        assertEquals(expected, javaInspector.print2(B.compilationUnit(),
                new DecoratorImpl(runtime, sourceSetOfRequest),
                javaInspector.importComputer(4, sourceSetOfRequest)));
    }

    @Language("java")
    private static final String INPUT95 = """
            package a.b;
            class X {
                static StringBuilder pattern(Object o) { if (o instanceof StringBuilder s) return s; return null; }
                static StringBuilder cast(Object o) { return (StringBuilder) o; }
                static StringBuilder castLocal(Object o) { StringBuilder x = (StringBuilder) o; return x; }
                static void viaPattern(Object o) { pattern(o).append("x"); }
                static void viaCast(Object o) { cast(o).append("x"); }
                static void viaCastLocal(Object o) { castLocal(o).append("x"); }
            }
            """;

    /*
    maddi#95: a downcast returned whole is the same object as its operand, and its summary now says so with the §m
    pair a pattern binding's return already had (only because a return inside a block is rebuilt through the
    shared-variable reconstruct). Every Kotlin smart cast converts to such a cast (#67).
    ⚠ The caller verdicts below held BEFORE the fix too (A/B with NORVCAST, 2026-09-29, also for a result kept in a
    local, a field argument, and an accessor downcasting a field): the whole-object ← already carries the
    modification. They guard that; the summary assertion is what the fix changed.
     */
    @DisplayName("modifying a downcast result modifies the operand")
    @Test
    public void test95() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT95);
        List<Info> ao = prepAnalyzer.doPrimaryType(X);
        analyzer.go(ao);

        for (String callee : List.of("pattern", "cast", "castLocal")) {
            MethodInfo m = X.findUniqueMethod(callee, 1);
            MethodLinkedVariables mlv = m.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertTrue(mlv.toString().contains(callee + ".§m≡0:o.§m"), callee + ": " + mlv);
        }
        for (String caller : List.of("viaPattern", "viaCast", "viaCastLocal")) {
            ParameterInfo o = X.findUniqueMethod(caller, 1).parameters().getFirst();
            assertTrue(o.isModified(), caller + ": o must be modified");
        }
    }
}
