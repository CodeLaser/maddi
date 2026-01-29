package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestIgnoreModifications extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X {
                void method() {
                    System.out.println("hi!");
                }
            }
            """;

    @DisplayName("ignore modification on System.out")
    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT1);
        TypeInfo system = javaInspector.compiledTypesManager().getOrLoad(System.class);
        FieldInfo out = system.getFieldByName("out", true);
        assertTrue(out.isIgnoreModifications());

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 0);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlvSet = tlc.doMethod(method);
        assertEquals("[] --> -", mlvSet.toString());

        VariableData vd = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viOut = vd.variableInfo("java.lang.System.out");
        assertFalse(viOut.isModified());

        assertTrue(mlvSet.modified().isEmpty());
    }

}
