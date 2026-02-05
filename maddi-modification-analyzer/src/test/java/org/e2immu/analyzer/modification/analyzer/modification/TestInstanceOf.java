package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInstanceOf extends CommonTest {

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
        List<Info> ao = prepAnalyzer.doPrimaryType(X);
        analyzer.go(ao);

        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo i = method.parameters().getFirst();
        assertTrue(i.isModified());
        MethodLinkedVariables mlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:i*≥1:s, 1:s≤0:i*] --> -", mlv.toString());
        assertEquals("a.b.X.method(a.b.X.I,String):0:i", mlv.sortedModifiedString());

        VariableData vd000 = VariableDataImpl.of(method.methodBody().statements().getLast().block().statements().getLast());
        VariableInfo viO00 = vd000.variableInfo("o");
        assertEquals("o.§es→set.§es,o.§es∋1:s,o.§m≡set.§m,o→set,o∩0:i", viO00.linkedVariables().toString());
        assertTrue(viO00.isModified());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getLast());
        assertEquals("""
                a.b.X.method(a.b.X.I,String):0:i, a.b.X.method(a.b.X.I,String):1:s, o, set\
                """, vd0.knownVariableNamesToString());
        VariableInfo viSet = vd0.variableInfo("set");
        assertEquals("set←o,set≺0:i", viSet.linkedVariables().toString());

        // what you see here is the 'eval' rather than the merge, because o and set have no merge
        VariableInfo viO = vd0.variableInfo("o");
        assertEquals("[java.util.Set]", viO.downcast().toString());
        assertEquals("o→set,o.§m≡set.§m,o≺0:i", viO.linkedVariables().toString());
        assertTrue(viO.isUnmodified());

        VariableInfo viI = vd0.variableInfo(i);
        assertEquals("[a.b.X.R]", viI.downcast().toString());
        assertEquals("0:i≻o,0:i≻set,0:i≥1:s,0:i∩o.§es,0:i∩set.§es", viI.linkedVariables().toString());
        assertTrue(viI.isModified());

        // TODO can we link the downcast of i->R to that of o to set?

        ValueImpl.VariableToTypeInfoSetImpl dc = i.analysis().getOrNull(PropertyImpl.DOWNCAST_PARAMETER,
                ValueImpl.VariableToTypeInfoSetImpl.class);
        assertEquals("{a.b.X.method(a.b.X.I,String):0:i=[a.b.X.R]}", dc.variableToTypeInfoSet().toString());

    }

}
