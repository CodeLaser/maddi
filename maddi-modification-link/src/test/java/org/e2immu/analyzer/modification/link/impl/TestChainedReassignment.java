package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Repro for the timefold-solver crash "X and Y should already have been removed; they're in the same equivalence
 * group" (Graph.mergeEdgeBi assert, sv == null branch): the ValueSelectorFactory.buildValueSelector shape, where a
 * local is repeatedly reassigned through wrapper methods that return their argument
 * ({@code v = filter(v); v = downcast(v); return v;}), across multiple return paths.
 * These tests only pin that link computation completes; the exact links are secondary.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestChainedReassignment extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C {
                interface Selector { void go(); }
                static class Impl implements Selector { int i; public void go() { i++; } }
                static class Wrapper implements Selector {
                    final Selector inner; int i;
                    Wrapper(Selector s) { this.inner = s; }
                    public void go() { i++; }
                }
                static class Factory {
                    static Factory create(String c) { return new Factory(); }
                    Selector buildValueSelector(String c) { return new Impl(); }
                }
                boolean flag;
                Selector mimic(String c) { return new Impl(); }
                // the ValueSelectorFactory shape: the wrapper CONDITIONALLY REASSIGNS ITS OWN PARAMETER
                Selector filter(Selector s) { if (flag) { s = new Wrapper(s); } return s; }
                Selector downcast(Selector s) { if (flag) { s = new Wrapper(s); } return s; }
                Selector base(String c) { return new Impl(); }
                Selector nearby(Selector s) { if (flag) { s = new Wrapper(s); } return s; }

                Selector wrapChain(String c) {
                    Selector valueSelector = base(c);
                    valueSelector = filter(valueSelector);
                    valueSelector = downcast(valueSelector);
                    return valueSelector;
                }

                Selector viaChainedScope(String c) {
                    Selector valueSelector = Factory.create(c).buildValueSelector(c);
                    valueSelector = filter(valueSelector);
                    return valueSelector;
                }

                Selector twoPaths(boolean b, String c) {
                    if (b) {
                        Selector valueSelector = mimic(c);
                        valueSelector = filter(valueSelector);
                        valueSelector = downcast(valueSelector);
                        return valueSelector;
                    }
                    Selector valueSelector = base(c);
                    valueSelector = nearby(valueSelector);
                    return valueSelector;
                }

                // the full ValueSelectorFactory.buildValueSelector shape: early-return branch with its own local,
                // then a long chain with an if/else where BOTH branches reassign, then further reassignments
                Selector buildValueSelector(boolean mimic, boolean near, String c) {
                    if (mimic) {
                        Selector valueSelector = mimic(c);
                        valueSelector = filter(valueSelector);
                        valueSelector = downcast(valueSelector);
                        return valueSelector;
                    }
                    Selector valueSelector = base(c);
                    if (near) {
                        valueSelector = nearby(valueSelector);
                    } else {
                        valueSelector = filter(valueSelector);
                    }
                    valueSelector = filter(valueSelector);
                    valueSelector = nearby(valueSelector);
                    valueSelector = downcast(valueSelector);
                    valueSelector = filter(valueSelector);
                    return valueSelector;
                }

                Selector twoPathsOneVariable(boolean b, String c) {
                    Selector valueSelector;
                    if (b) {
                        valueSelector = mimic(c);
                        valueSelector = filter(valueSelector);
                        valueSelector = downcast(valueSelector);
                        return valueSelector;
                    }
                    valueSelector = base(c);
                    valueSelector = nearby(valueSelector);
                    return valueSelector;
                }
            }
            """;

    private Map<String, MethodLinkedVariables> cache;

    private MethodLinkedVariables mlv(String methodName) {
        if (cache == null) {
            TypeInfo c = javaInspector.parse("a.b.C", INPUT);
            new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(c);
            LinkComputerImpl lc = new LinkComputerImpl(javaInspector);
            cache = new HashMap<>();
            c.recursiveSubTypeStream().flatMap(TypeInfo::methodStream)
                    .forEach(m -> cache.put(m.name(), lc.doMethod(m)));
            c.methodStream().forEach(m -> cache.put(m.name(), lc.doMethod(m)));
        }
        return cache.get(methodName);
    }

    @DisplayName("wrapper-chain reassignment completes")
    @Test
    public void wrapChain() {
        assertNotNull(mlv("wrapChain"));
    }

    @DisplayName("chained-scope call result, then wrapper reassignment, completes")
    @Test
    public void viaChainedScope() {
        assertNotNull(mlv("viaChainedScope"));
    }

    @DisplayName("two return paths, separate locals, completes")
    @Test
    public void twoPaths() {
        assertNotNull(mlv("twoPaths"));
    }

    @DisplayName("two return paths through one local, completes")
    @Test
    public void twoPathsOneVariable() {
        assertNotNull(mlv("twoPathsOneVariable"));
    }

    @DisplayName("full buildValueSelector shape: early return + if/else both-branch reassignment + chain, completes")
    @Test
    public void buildValueSelector() {
        assertNotNull(mlv("buildValueSelector"));
    }
}
