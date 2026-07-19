package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.Info;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAN-modification-reachability phase 0: the deep capture chain. An object captured in a field,
 * passed constructor-to-constructor five levels deep, modified only at the sink — every FCi.&lt;init&gt;
 * parameter and every FCi.f must be modified. Today modification saturates two levels above the sink
 * (premature optimistic writes frozen by the upward-only overwrite policy; certification is blind to
 * the refusals — see the STRICTCERT counter, commit 261a22e4). This is the red test the reachability
 * pass must turn green.
 */
public class TestDeepCaptureChain extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            class X {
                static class FC1 { List<Integer> f; FC1(List<Integer> in) { this.f = in; } void pass() { new FC2(this.f).pass(); } }
                static class FC2 { List<Integer> f; FC2(List<Integer> in) { this.f = in; } void pass() { new FC3(this.f).pass(); } }
                static class FC3 { List<Integer> f; FC3(List<Integer> in) { this.f = in; } void pass() { new FC4(this.f).pass(); } }
                static class FC4 { List<Integer> f; FC4(List<Integer> in) { this.f = in; } void pass() { new FC5(this.f).pass(); } }
                static class FC5 { List<Integer> f; FC5(List<Integer> in) { this.f = in; } void pass() { SINK.receive(this.f); } }
                static class SINK { static void receive(List<Integer> r) { r.removeFirst(); } }
            }
            """;

    @DisplayName("deep capture chain: modification must reach every level")
    @Test
    public void test() {
        // GREEN since P2.3a (was RED by design, phase 0): under modificationViaReachability the
        // post-convergence pass propagates the sink modification through every capture level,
        // downgrading the frozen optimistic TRUEs the iterating fixpoint certified.
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> ao = prepWork(X);
        var iterating = new org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl(javaInspector,
                new org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setMaxIterations(10)
                        .setModificationViaReachability(true) // implies trackObjectCreations
                        .build());
        iterating.analyze(ao);
        for (int i = 1; i <= 5; i++) {
            TypeInfo fc = X.findSubType("FC" + i);
            MethodInfo ctor = fc.constructors().getFirst();
            assertFalse(ctor.parameters().getFirst().isUnmodified(),
                    "FC" + i + ".<init> parameter must be modified");
            assertFalse(fc.getFieldByName("f", true).isUnmodified(),
                    "FC" + i + ".f must be modified");
        }
    }
}
