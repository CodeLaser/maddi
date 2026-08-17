package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The link computer must never source-compute a method whose statements carry no prep data: doStatement's
 * {@code assert vd != null} kills not just that method but every caller whose computation recursed into it,
 * a silent abort ("Caught exception computing ...") that loses the caller's METHOD_LINKS entirely.
 * <p>
 * Prepwork can leave a reachable source method without VariableData: a fault-tolerant prep run isolates a
 * failing type or method and carries on ({@code PrepAnalyzer.doType} / {@code doMethodIsolated}), and a
 * caller prepping one primary type at a time may simply never have prepped the callee's type — the shape in
 * which jfocus-standardize's intake lost 208 closed-core methods to this abort (see
 * {@code docs/handoff-linkcomputer-recursion-vd-null.md}).
 * <p>
 * Pinned behaviour: {@code LinkComputerImpl.doMethod} detects the missing prep data, degrades THAT method to
 * a shallow summary — explicitly, marking {@code DEGRADED_ANALYSIS_METHOD} — and the caller completes
 * normally.
 */
public class TestLinkUnpreppedCallee extends CommonTest {

    @Language("java")
    private static final String CALLER = """
            package a.b;
            public class Caller {
                public String go() {
                    String x = Callee.sep();
                    return x;
                }
            }
            """;

    @Language("java")
    private static final String CALLEE = """
            package a.b;
            public class Callee {
                public static String sep() {
                    String s = System.lineSeparator();
                    return s;
                }
            }
            """;

    @DisplayName("recursion into an unprepped callee: shallow summary + degradation marker, caller completes")
    @Test
    public void callerOverUnpreppedCallee() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.Caller", CALLER, "a.b.Callee", CALLEE),
                javaInspector.failFast()).parseResult();
        TypeInfo caller = parseResult.findType("a.b.Caller");
        TypeInfo callee = parseResult.findType("a.b.Callee");
        prepWork(caller); // Callee deliberately NOT prepped

        MethodInfo go = caller.findUniqueMethod("go", 0);
        // before the guard in LinkComputerImpl.doMethod, this threw AssertionError (doStatement, 'assert vd != null')
        MethodLinkedVariables mlv = new LinkComputerImpl(javaInspector).doMethod(go);
        assertNotNull(mlv);

        MethodInfo sep = callee.findUniqueMethod("sep", 0);
        assertTrue(sep.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.FALSE)
                .isTrue(), "the unprepped callee must be explicitly marked degraded");
        assertNotNull(sep.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class),
                "the callee's shallow summary must have been stored by the recursion's getOrCreate");
        assertFalse(go.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.FALSE)
                .isTrue(), "the caller's own analysis is complete; it must not carry the marker");
        assertTrue(go.analysis().haveAnalyzedValueFor(PropertyImpl.NON_MODIFYING_METHOD),
                "proof the caller's computation ran to the end");
    }

    @DisplayName("direct doMethod on an unprepped method: shallow summary instead of AssertionError")
    @Test
    public void directOnUnpreppedMethod() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.Caller", CALLER, "a.b.Callee", CALLEE),
                javaInspector.failFast()).parseResult();
        TypeInfo callee = parseResult.findType("a.b.Callee");
        // no prep at all

        MethodInfo sep = callee.findUniqueMethod("sep", 0);
        MethodLinkedVariables mlv = new LinkComputerImpl(javaInspector).doMethod(sep);
        assertNotNull(mlv);
        assertTrue(sep.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.FALSE)
                .isTrue());
    }

    @DisplayName("control: both types prepped, nothing is degraded")
    @Test
    public void bothPrepped() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.Caller", CALLER, "a.b.Callee", CALLEE),
                javaInspector.failFast()).parseResult();
        TypeInfo caller = parseResult.findType("a.b.Caller");
        TypeInfo callee = parseResult.findType("a.b.Callee");
        prepWork(caller);
        prepAnalyzer.doPrimaryType(callee);

        MethodInfo go = caller.findUniqueMethod("go", 0);
        MethodLinkedVariables mlv = new LinkComputerImpl(javaInspector).doMethod(go);
        assertNotNull(mlv);

        MethodInfo sep = callee.findUniqueMethod("sep", 0);
        assertFalse(sep.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.FALSE)
                .isTrue());
        assertFalse(go.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.FALSE)
                .isTrue());
    }
}
