package org.e2immu.analyzer.modification.linkedvariables.cast;

import org.e2immu.analyzer.modification.io.DecoratorImpl;
import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
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
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.DOWNCAST_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.SetOfTypeInfoImpl.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCast extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
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

    @DisplayName("simple instanceof")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo methodInfo = B.findUniqueMethod("method", 1);
        ParameterInfo pi0 = methodInfo.parameters().getFirst();
        Statement s0If = methodInfo.methodBody().statements().getFirst();
        Statement s000Add = s0If.block().statements().getFirst();
        VariableData vd000 = VariableDataImpl.of(s000Add);
        VariableInfo vi000Set = vd000.variableInfo("set");
        assertTrue(vi000Set.isModified());
        VariableData vd0 = VariableDataImpl.of(s0If);
        VariableInfo vi0Object = vd0.variableInfo(pi0);
        Value.SetOfTypeInfo downcastsVi = vi0Object.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE, EMPTY);
        assertTrue(vi0Object.isModified());
        assertEquals("[java.util.Set]", downcastsVi.typeInfoSet().toString());
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
        assertEquals(expected, javaInspector.print2(B, new DecoratorImpl(runtime, sourceSetOfRequest),
                javaInspector.importComputer(4, sourceSetOfRequest)));
    }
}
