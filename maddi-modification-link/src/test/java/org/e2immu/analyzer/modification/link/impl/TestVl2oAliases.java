package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #38 (extract-interface adequacy review, 2026-07-18): the ≡ exclusion in addVL2O is deliberate
 * and CORRECT for declared-type consumers, and this test pins both sides of the line:
 * <ul>
 * <li>an UNMEDIATED alias (plain assignment chain) reaches VL2O through its ← link — rejection blast
 *     radius includes it;</li>
 * <li>a MEDIATED alias (two variables produced by SEPARATE casts of the same source, linked only by
 *     real-variable ≡) is deliberately absent: each cast re-mediates the declared type, so the aliases
 *     are runtime-identical but type-decoupled. Coupling them would be the mediation-blindness mistake
 *     documented in the task-#39 premise revision.</li>
 * </ul>
 */
public class TestVl2oAliases extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b.ii;
            class C1 {
                interface II {
                    void method1(String s);
                    void method2(int i);
                }
                void unmediated(II a, String s) {
                    II b = a;
                    b.method2(1);
                }
                void mediated(Object o, String s) {
                    II ii = (II) o;
                    ii.method1(s);
                    II ii2 = (II) o;
                    ii2.method2(1);
                }
            }
            """;

    @DisplayName("unmediated alias reaches VL2O; separately-cast (≡-only) alias deliberately does not")
    @Test
    public void test() {
        TypeInfo C1 = javaInspector.parse("a.b.ii.C1", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(C1);
        LinkComputer lc = new LinkComputerImpl(javaInspector);

        MethodInfo unmediated = C1.findUniqueMethod("unmediated", 2);
        MethodLinkedVariables mlvU = unmediated.analysis().getOrCreate(METHOD_LINKS, () -> lc.doMethod(unmediated));
        assertNotNull(mlvU);
        MethodCall callU = (MethodCall) unmediated.methodBody().statements().getLast().expression();
        ValueImpl.VariableBooleanMapImpl vbmU = callU.analysis().getOrNull(VARIABLES_LINKED_TO_OBJECT,
                ValueImpl.VariableBooleanMapImpl.class);
        assertNotNull(vbmU);
        assertTrue(vbmU.map().keySet().stream().anyMatch(v -> "a".equals(v.simpleName())),
                "the plain-assignment alias 'a' must be in the blast radius: " + vbmU);

        MethodInfo mediated = C1.findUniqueMethod("mediated", 2);
        MethodLinkedVariables mlvM = mediated.analysis().getOrCreate(METHOD_LINKS, () -> lc.doMethod(mediated));
        assertNotNull(mlvM);
        MethodCall callM = (MethodCall) mediated.methodBody().statements().getLast().expression();
        ValueImpl.VariableBooleanMapImpl vbmM = callM.analysis().getOrNull(VARIABLES_LINKED_TO_OBJECT,
                ValueImpl.VariableBooleanMapImpl.class);
        assertNotNull(vbmM);
        assertTrue(vbmM.map().keySet().stream().anyMatch(v -> "ii2".equals(v.simpleName())),
                "the receiver's own cast alias must be present: " + vbmM);
        assertFalse(vbmM.map().keySet().stream().anyMatch(v -> "ii".equals(v.simpleName())),
                "the separately-cast ≡-only alias 'ii' must stay OUT (type-decoupled by its own cast): " + vbmM);
    }
}
